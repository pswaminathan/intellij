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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.idea.blaze.common.Context;
import com.google.idea.blaze.common.Output;
import com.google.idea.blaze.qsync.project.ProjectProto;
import com.google.idea.blaze.qsync.query.QuerySummary;
import com.google.idea.blaze.qsync.vcs.VcsState;
import java.util.Optional;

/** Helpers for unit tests. */
public class QuerySyncTestUtils {

  private QuerySyncTestUtils() {}

  public static final Context NOOP_CONTEXT =
      new Context() {
        @Override
        public <T extends Output> void output(T output) {}

        @Override
        public void setHasError() {}
      };

  public static final PackageReader EMPTY_PACKAGE_READER = p -> "";

  public static final Optional<VcsState> CLEAN_VCS_STATE =
      Optional.of(new VcsState("1", ImmutableSet.of()));

  public static BlazeProjectSnapshot.Builder emptyProjectBuilder() {
    return BlazeProjectSnapshot.builder()
        .projectIncludes(ImmutableList.of())
        .projectExcludes(ImmutableList.of())
        .queryOutput(QuerySummary.EMPTY)
        .graph(BuildGraphData.EMPTY)
        .vcsState(Optional.empty())
        .project(ProjectProto.Project.getDefaultInstance());
  }
}
