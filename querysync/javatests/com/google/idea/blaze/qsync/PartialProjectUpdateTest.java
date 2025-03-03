/*
 * Copyright 2023 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.qsync;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.idea.blaze.qsync.QuerySyncTestUtils.emptyProjectBuilder;

import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.common.Label;
import com.google.idea.blaze.qsync.query.Query;
import com.google.idea.blaze.qsync.query.QuerySummary;
import java.nio.file.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PartialProjectUpdateTest {

  @Test
  public void testApplyDelta_replacePackage() {
    QuerySummary base =
        QuerySummary.create(
            Query.Summary.newBuilder()
                .putRules(
                    "//my/build/package1:rule",
                    Query.Rule.newBuilder()
                        .setRuleClass("java_library")
                        .addSources("//my/build/package1:Class1.java")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:Class1.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/Class1.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:subpackage/AnotherClass.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/subpackage/AnotherClass.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:BUILD",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/BUILD:1:1")
                        .build())
                .putRules(
                    "//my/build/package2:rule",
                    Query.Rule.newBuilder()
                        .setRuleClass("java_library")
                        .addSources("//my/build/package2:Class2.java")
                        .build())
                .putSourceFiles(
                    "//my/build/package2:Class2.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package2/Class2.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package2:BUILD",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package2/BUILD:1:1")
                        .build())
                .build());
    BlazeProjectSnapshot baseProject = emptyProjectBuilder().queryOutput(base).build();

    QuerySummary delta =
        QuerySummary.create(
            Query.Summary.newBuilder()
                .putRules(
                    "//my/build/package1:newrule",
                    Query.Rule.newBuilder()
                        .setRuleClass("java_library")
                        .addSources("//my/build/package1:NewClass.java")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:NewClass.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/NewClass.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:BUILD",
                    Query.SourceFile.newBuilder().setLocation("my/build/package1/BUILD").build())
                .build());

    PartialProjectUpdate queryStrategy =
        new PartialProjectUpdate(
            QuerySyncTestUtils.NOOP_CONTEXT,
            QuerySyncTestUtils.EMPTY_PACKAGE_READER,
            baseProject,
            QuerySyncTestUtils.CLEAN_VCS_STATE,
            /* modifiedPackages= */ ImmutableSet.of(Path.of("my/build/package1")),
            ImmutableSet.of());
    queryStrategy.setQueryOutput(delta);
    QuerySummary applied = queryStrategy.applyDelta();
    assertThat(applied.getRulesMap().keySet())
        .containsExactly(
            Label.of("//my/build/package1:newrule"), Label.of("//my/build/package2:rule"));
    assertThat(applied.getSourceFilesMap().keySet())
        .containsExactly(
            Label.of("//my/build/package1:NewClass.java"),
            Label.of("//my/build/package1:BUILD"),
            Label.of("//my/build/package2:Class2.java"),
            Label.of("//my/build/package2:BUILD"));
  }

  @Test
  public void testApplyDelta_deletePackage() {
    QuerySummary base =
        QuerySummary.create(
            Query.Summary.newBuilder()
                .putRules(
                    "//my/build/package1:rule",
                    Query.Rule.newBuilder()
                        .setRuleClass("java_library")
                        .addSources("//my/build/package1:Class1.java")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:Class1.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/Class1.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:subpackage/AnotherClass.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/subpackage/AnotherClass.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:BUILD",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/BUILD:1:1")
                        .build())
                .putRules(
                    "//my/build/package2:rule",
                    Query.Rule.newBuilder()
                        .setRuleClass("java_library")
                        .addSources("//my/build/package2:Class2.java")
                        .build())
                .putSourceFiles(
                    "//my/build/package2:Class2.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package2/Class2.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package2:BUILD",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package2/BUILD:1:1")
                        .build())
                .build());
    BlazeProjectSnapshot baseProject = emptyProjectBuilder().queryOutput(base).build();

    PartialProjectUpdate queryStrategy =
        new PartialProjectUpdate(
            QuerySyncTestUtils.NOOP_CONTEXT,
            QuerySyncTestUtils.EMPTY_PACKAGE_READER,
            baseProject,
            QuerySyncTestUtils.CLEAN_VCS_STATE,
            ImmutableSet.of(),
            /* deletedPackages= */ ImmutableSet.of(Path.of("my/build/package1")));
    assertThat(queryStrategy.getQuerySpec()).isEmpty();
    queryStrategy.setQueryOutput(QuerySummary.EMPTY);
    QuerySummary applied = queryStrategy.applyDelta();
    assertThat(applied.getRulesMap().keySet())
        .containsExactly(Label.of("//my/build/package2:rule"));
    assertThat(applied.getSourceFilesMap().keySet())
        .containsExactly(
            Label.of("//my/build/package2:Class2.java"), Label.of("//my/build/package2:BUILD"));
  }

  @Test
  public void testDelta_addPackage() {
    QuerySummary base =
        QuerySummary.create(
            Query.Summary.newBuilder()
                .putRules(
                    "//my/build/package1:rule",
                    Query.Rule.newBuilder()
                        .setRuleClass("java_library")
                        .addSources("//my/build/package1:Class1.java")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:Class1.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/Class1.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package1:BUILD",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package1/BUILD:1:1")
                        .build())
                .build());
    BlazeProjectSnapshot baseProject = emptyProjectBuilder().queryOutput(base).build();
    QuerySummary delta =
        QuerySummary.create(
            Query.Summary.newBuilder()
                .putRules(
                    "//my/build/package2:rule",
                    Query.Rule.newBuilder()
                        .setRuleClass("java_library")
                        .addSources("//my/build/package2:Class2.java")
                        .build())
                .putSourceFiles(
                    "//my/build/package2:Class2.java",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package2/Class2.java:1:1")
                        .build())
                .putSourceFiles(
                    "//my/build/package2:BUILD",
                    Query.SourceFile.newBuilder()
                        .setLocation("my/build/package2/BUILD:1:1")
                        .build())
                .build());

    PartialProjectUpdate queryStrategy =
        new PartialProjectUpdate(
            QuerySyncTestUtils.NOOP_CONTEXT,
            QuerySyncTestUtils.EMPTY_PACKAGE_READER,
            baseProject,
            QuerySyncTestUtils.CLEAN_VCS_STATE,
            /* modifiedPackages= */ ImmutableSet.of(Path.of("my/build/package2")),
            ImmutableSet.of());
    queryStrategy.setQueryOutput(delta);
    QuerySummary applied = queryStrategy.applyDelta();
    assertThat(applied.getRulesMap().keySet())
        .containsExactly(
            Label.of("//my/build/package1:rule"), Label.of("//my/build/package2:rule"));
    assertThat(applied.getSourceFilesMap().keySet())
        .containsExactly(
            Label.of("//my/build/package1:Class1.java"),
            Label.of("//my/build/package1:BUILD"),
            Label.of("//my/build/package2:Class2.java"),
            Label.of("//my/build/package2:BUILD"));
  }
}
