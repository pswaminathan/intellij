"""An aspect to gather info needed by the FastBuildService."""

load(
    ":artifacts.bzl",
    "artifact_location",
    "sources_from_target",
    "struct_omit_none",
)
load(
    ":intellij_info_impl.bzl",
    "stringify_label",
)

_DEP_ATTRS = ["deps", "exports", "runtime_deps", "_java_toolchain"]

def _fast_build_info_impl(target, ctx):
    undo_unambiguous_label_stringification = ctx.attr.undo_unambiguous_label_stringification
    dep_targets = _get_all_dep_targets(target, ctx)
    dep_outputs = _get_all_dep_outputs(dep_targets)

    output_files = []

    info = {
        "workspace_name": ctx.workspace_name,
        "label": stringify_label(target.label, undo_unambiguous_label_stringification),
        "dependencies": [stringify_label(t.label, undo_unambiguous_label_stringification) for t in dep_targets],
    }

    write_output = False
    if hasattr(ctx.rule.attr, "data") and ctx.rule.attr.data:
        # The data attribute can reference artifacts directly (like deploy jars) that the aspect
        # will skip. So we need to gather them up here, in the referencing target.
        write_output = True
        info["data"] = [
            struct(
                label = stringify_label(datadep.label, undo_unambiguous_label_stringification),
                artifacts = [artifact_location(file) for file in datadep.files.to_list()],
            )
            for datadep in ctx.rule.attr.data
        ]

    if hasattr(target, "java_toolchain"):
        toolchain = target.java_toolchain
    elif java_common.JavaToolchainInfo != platform_common.ToolchainInfo and \
         java_common.JavaToolchainInfo in target:
        toolchain = target[java_common.JavaToolchainInfo]
    else:
        toolchain = None
    if toolchain:
        write_output = True
        javac_jars = []
        if hasattr(toolchain, "tools"):
            javac_jars = [artifact_location(f) for f in toolchain.tools.to_list()]
        bootclasspath_jars = []
        if hasattr(toolchain, "bootclasspath"):
            bootclasspath_jars = [artifact_location(f) for f in toolchain.bootclasspath.to_list()]
        info["java_toolchain_info"] = struct_omit_none(
            javac_jars = javac_jars,
            bootclasspath_jars = bootclasspath_jars,
            source_version = toolchain.source_version,
            target_version = toolchain.target_version,
        )
    if JavaInfo in target:
        write_output = True
        launcher = None
        if hasattr(ctx.rule.attr, "use_launcher") and not ctx.rule.attr.use_launcher:
            launcher = None
        elif hasattr(ctx.rule.attr, "_java_launcher") and ctx.rule.attr._java_launcher:
            launcher = stringify_label(ctx.rule.attr._java_launcher.label, undo_unambiguous_label_stringification)
        elif hasattr(ctx.rule.attr, "_javabase") and ctx.rule.attr._javabase:
            launcher = stringify_label(ctx.rule.attr._javabase.label, undo_unambiguous_label_stringification)
        java_info = {
            "sources": sources_from_target(ctx),
            "test_class": getattr(ctx.rule.attr, "test_class", None),
            "test_size": getattr(ctx.rule.attr, "size", None),
            "launcher": launcher,
            "swigdeps": getattr(ctx.rule.attr, "swigdeps", True),
            "jvm_flags": getattr(ctx.rule.attr, "jvm_flags", []),
        }
        annotation_processing = target[JavaInfo].annotation_processing
        if annotation_processing:
            java_info["annotation_processor_class_names"] = annotation_processing.processor_classnames
            java_info["annotation_processor_classpath"] = [
                artifact_location(t)
                for t in annotation_processing.processor_classpath.to_list()
            ]
        info["java_info"] = struct_omit_none(**java_info)
    if hasattr(target, "android"):
        write_output = True
        android_info = struct_omit_none(
            aar = artifact_location(target.android.aar),
            merged_manifest = artifact_location(target.android.merged_manifest),
        )
        info["android_info"] = android_info

    if write_output:
        output_file = ctx.actions.declare_file(target.label.name + ".ide-fast-build-info.txt")
        ctx.actions.write(output_file, struct_omit_none(**info).to_proto())
        output_files += [output_file]

    output_groups = depset(output_files, transitive = dep_outputs)

    return struct(output_groups = {"ide-fast-build": output_groups})

def _get_all_dep_outputs(dep_targets):
    """Get the ide-fast-build output files for all dependencies"""
    return [
        dep_target[OutputGroupInfo]["ide-fast-build"]
        for dep_target in dep_targets
        if _has_ide_fast_build(dep_target)
    ]

def _get_all_dep_targets(target, ctx):
    """Get all the targets mentioned in one of the _DEP_ATTRS attributes of the target"""
    targets = []
    for attr_name in _DEP_ATTRS:
        attr_val = getattr(ctx.rule.attr, attr_name, None)
        if not attr_val:
            continue
        attr_type = type(attr_val)
        if attr_type == type(target):
            targets.append(attr_val)
        elif attr_type == type([]):
            targets += [list_val for list_val in attr_val if type(list_val) == type(target)]
    return targets

def _has_ide_fast_build(target):
    return OutputGroupInfo in target and "ide-fast-build" in target[OutputGroupInfo]

fast_build_info_aspect = aspect(
    attr_aspects = _DEP_ATTRS,
    implementation = _fast_build_info_impl,
    attrs = {
        # If undo_unambiguous_label_stringification is set to true, remove the `@` or `@@`
        # from the beginning of targets labels obtained by str(target.label) otherwise use the
        # result of str(target.label) directly.
        # This attribute should be true only during tests.
        # This attribute is added as a workaround to keep the test code similar for internal and
        # external tests until --incompatible_unambiguous_label_stringification Blaze flag
        # is enabled internally.
        "undo_unambiguous_label_stringification": attr.string(default = "False", values = ["True", "False"]),
    },
)
