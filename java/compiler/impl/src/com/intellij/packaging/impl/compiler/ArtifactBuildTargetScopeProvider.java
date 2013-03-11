/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.packaging.impl.compiler;

import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.compiler.impl.CompileScopeUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerFilter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;
import org.jetbrains.jps.incremental.artifacts.ArtifactBuildTargetType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class ArtifactBuildTargetScopeProvider extends BuildTargetScopeProvider {
  @NotNull
  @Override
  public List<TargetTypeBuildScope> getBuildTargetScopes(@NotNull final CompileScope baseScope, @NotNull CompilerFilter filter,
                                                         @NotNull final Project project, final boolean forceBuild) {
    final ArtifactsCompiler compiler = ArtifactsCompiler.getInstance(project);
    if (compiler == null || !filter.acceptCompiler(compiler)) {
      return Collections.emptyList();
    }
    final List<TargetTypeBuildScope> scopes = new ArrayList<TargetTypeBuildScope>();
    new ReadAction() {
      protected void run(final Result result) {
        final Set<Artifact> artifacts = ArtifactCompileScope.getArtifactsToBuild(project, baseScope, false);
        if (ArtifactCompileScope.getArtifacts(baseScope) == null) {
          Set<Module> modules = ArtifactUtil.getModulesIncludedInArtifacts(artifacts, project);
          CompileScopeUtil.addScopesForModules(modules, scopes, forceBuild);
        }
        if (!artifacts.isEmpty()) {
          TargetTypeBuildScope.Builder builder = TargetTypeBuildScope.newBuilder()
            .setTypeId(ArtifactBuildTargetType.INSTANCE.getTypeId())
            .setForceBuild(ArtifactCompileScope.isArtifactRebuildForced(baseScope));
          for (Artifact artifact : artifacts) {
            builder.addTargetId(artifact.getName());
          }
          scopes.add(builder.build());
        }
      }
    }.execute();

    return scopes;
  }
}
