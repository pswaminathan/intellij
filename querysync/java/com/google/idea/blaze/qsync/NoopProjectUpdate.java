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

import com.google.idea.blaze.qsync.query.QuerySpec;
import com.google.idea.blaze.qsync.query.QuerySummary;
import java.util.Optional;

/** Query strategy used when no update is necessary. Simply returns the previous project state. */
class NoopProjectUpdate implements ProjectUpdate {

  private final BlazeProjectSnapshot project;

  public NoopProjectUpdate(BlazeProjectSnapshot project) {
    this.project = project;
  }

  @Override
  public void setQueryOutput(QuerySummary output) {}

  @Override
  public Optional<QuerySpec> getQuerySpec() {
    return Optional.empty();
  }

  @Override
  public BlazeProjectSnapshot createBlazeProject() {
    return project;
  }
}
