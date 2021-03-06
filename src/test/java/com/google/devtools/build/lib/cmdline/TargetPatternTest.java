// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.cmdline;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.devtools.build.lib.cmdline.TargetPattern.Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link TargetPattern}.
 */
@RunWith(JUnit4.class)
public class TargetPatternTest {

  @Test
  public void testPassingValidations() throws TargetParsingException {
    parse("foo:bar");
    parse("foo:all");
    parse("foo/...:all");
    parse("foo:*");

    parse("//foo");
    parse("//foo:bar");
    parse("//foo:all");

    parse("//foo/all");
    parse("java/com/google/foo/Bar.java");
    parse("//foo/...:all");

    parse("//...");
    parse("@repo//foo:bar");
    parse("@repo//foo:all");
    parse("@repo//:bar");
  }

  @Test
  public void testInvalidPatterns() throws TargetParsingException {
    try {
      parse("Bar&&&java");
      fail();
    } catch (TargetParsingException expected) {
    }
  }

  @Test
  public void testNormalize() {
    // Good cases.
    assertThat(TargetPattern.normalize("empty")).isEqualTo("empty");
    assertThat(TargetPattern.normalize("a/b")).isEqualTo("a/b");
    assertThat(TargetPattern.normalize("a/b/c")).isEqualTo("a/b/c");
    assertThat(TargetPattern.normalize("a/b/c.d")).isEqualTo("a/b/c.d");
    assertThat(TargetPattern.normalize("a/b/c..")).isEqualTo("a/b/c..");
    assertThat(TargetPattern.normalize("a/b/c...")).isEqualTo("a/b/c...");

    assertThat(TargetPattern.normalize("a/b/")).isEqualTo("a/b"); // Remove trailing empty segments
    assertThat(TargetPattern.normalize("a//c")).isEqualTo("a/c"); // Remove empty inner segments
    assertThat(TargetPattern.normalize("a/./d")).isEqualTo("a/d"); // Remove inner dot segments
    assertThat(TargetPattern.normalize("a/.")).isEqualTo("a"); // Remove trailing dot segments
    // Remove .. segment and its predecessor
    assertThat(TargetPattern.normalize("a/b/../e")).isEqualTo("a/e");
    // Remove trailing .. segment and its predecessor
    assertThat(TargetPattern.normalize("a/g/b/..")).isEqualTo("a/g");
    // Remove double .. segments and two predecessors
    assertThat(TargetPattern.normalize("a/b/c/../../h")).isEqualTo("a/h");
    // Don't remove leading .. segments
    assertThat(TargetPattern.normalize("../a")).isEqualTo("../a");
    assertThat(TargetPattern.normalize("../../a")).isEqualTo("../../a");
    assertThat(TargetPattern.normalize("../../../a")).isEqualTo("../../../a");
    assertThat(TargetPattern.normalize("a/../../../b")).isEqualTo("../../b");
  }

  @Test
  public void testTargetsBelowDirectoryContainsNestedPatterns() throws Exception {
    // Given an outer pattern '//foo/...',
    TargetPattern outerPattern = parseAsExpectedType("//foo/...", Type.TARGETS_BELOW_DIRECTORY);
    // And a nested inner pattern '//foo/bar/...',
    TargetPattern innerPattern = parseAsExpectedType("//foo/bar/...", Type.TARGETS_BELOW_DIRECTORY);
    // Then the outer pattern contains the inner pattern,,
    assertThat(outerPattern.containsDirectoryOfTBDForTBD(innerPattern)).isTrue();
    // And the inner pattern does not contain the outer pattern.
    assertThat(innerPattern.containsDirectoryOfTBDForTBD(outerPattern)).isFalse();
  }

  @Test
  public void testTargetsBelowDirectoryIsExcludableFromForIndependentPatterns() throws Exception {
    // Given a pattern '//foo/...',
    TargetPattern patternFoo = parseAsExpectedType("//foo/...", Type.TARGETS_BELOW_DIRECTORY);
    // And a pattern '//bar/...',
    TargetPattern patternBar = parseAsExpectedType("//bar/...", Type.TARGETS_BELOW_DIRECTORY);
    // Then neither pattern contains the other.
    assertThat(patternFoo.containsDirectoryOfTBDForTBD(patternBar)).isFalse();
    assertThat(patternBar.containsDirectoryOfTBDForTBD(patternFoo)).isFalse();
  }

  @Test
  public void testTargetsBelowDirectoryContainsForOtherPatternTypes() throws Exception {
    // Given a TargetsBelowDirectory pattern, tbdFoo of '//foo/...',
    TargetPattern tbdFoo = parseAsExpectedType("//foo/...", Type.TARGETS_BELOW_DIRECTORY);

    // And target patterns of each type other than TargetsBelowDirectory, e.g. 'foo/bar',
    // '//foo:bar', and 'foo:all',
    TargetPattern pathAsTargetPattern = parseAsExpectedType("foo/bar", Type.PATH_AS_TARGET);
    TargetPattern singleTargetPattern = parseAsExpectedType("//foo:bar", Type.SINGLE_TARGET);
    TargetPattern targetsInPackagePattern = parseAsExpectedType("foo:all", Type.TARGETS_IN_PACKAGE);

    // Then the non-TargetsBelowDirectory patterns do not contain tbdFoo.
    assertThat(pathAsTargetPattern.containsDirectoryOfTBDForTBD(tbdFoo)).isFalse();
    // And are not considered to be a contained directory of the TargetsBelowDirectory pattern.
    assertThat(tbdFoo.containsDirectoryOfTBDForTBD(pathAsTargetPattern)).isFalse();

    assertThat(singleTargetPattern.containsDirectoryOfTBDForTBD(tbdFoo)).isFalse();
    assertThat(tbdFoo.containsDirectoryOfTBDForTBD(singleTargetPattern)).isFalse();

    assertThat(targetsInPackagePattern.containsDirectoryOfTBDForTBD(tbdFoo)).isFalse();
    assertThat(tbdFoo.containsDirectoryOfTBDForTBD(targetsInPackagePattern)).isFalse();
  }

  @Test
  public void testTargetsBelowDirectoryDoesNotContainCoincidentPrefixPatterns() throws Exception {
    // Given a TargetsBelowDirectory pattern, tbdFoo of '//foo/...',
    TargetPattern tbdFoo = parseAsExpectedType("//foo/...", Type.TARGETS_BELOW_DIRECTORY);

    // And target patterns with prefixes equal to the directory of the TBD pattern, but not below
    // it,
    TargetPattern targetsBelowDirectoryPattern =
        parseAsExpectedType("//food/...", Type.TARGETS_BELOW_DIRECTORY);
    TargetPattern pathAsTargetPattern = parseAsExpectedType("food/bar", Type.PATH_AS_TARGET);
    TargetPattern singleTargetPattern = parseAsExpectedType("//food:bar", Type.SINGLE_TARGET);
    TargetPattern targetsInPackagePattern =
        parseAsExpectedType("food:all", Type.TARGETS_IN_PACKAGE);

    // Then the non-TargetsBelowDirectory patterns are not contained by tbdFoo.
    assertThat(tbdFoo.containsDirectoryOfTBDForTBD(targetsBelowDirectoryPattern)).isFalse();
    assertThat(tbdFoo.containsDirectoryOfTBDForTBD(pathAsTargetPattern)).isFalse();
    assertThat(tbdFoo.containsDirectoryOfTBDForTBD(singleTargetPattern)).isFalse();
    assertThat(tbdFoo.containsDirectoryOfTBDForTBD(targetsInPackagePattern)).isFalse();
  }

  @Test
  public void testDepotRootTargetsBelowDirectoryContainsPatterns() throws Exception {
    // Given a TargetsBelowDirectory pattern, tbdDepot of '//...',
    TargetPattern tbdDepot = parseAsExpectedType("//...", Type.TARGETS_BELOW_DIRECTORY);

    // And target patterns of each type other than TargetsBelowDirectory, e.g. 'foo/bar',
    // '//foo:bar', and 'foo:all',
    TargetPattern tbdFoo = parseAsExpectedType("//foo/...", Type.TARGETS_BELOW_DIRECTORY);
    TargetPattern pathAsTargetPattern = parseAsExpectedType("foo/bar", Type.PATH_AS_TARGET);
    TargetPattern singleTargetPattern = parseAsExpectedType("//foo:bar", Type.SINGLE_TARGET);
    TargetPattern targetsInPackagePattern = parseAsExpectedType("foo:all", Type.TARGETS_IN_PACKAGE);

    // Then the patterns are contained by tbdDepot, and do not contain tbdDepot.
    assertThat(tbdDepot.containsDirectoryOfTBDForTBD(tbdFoo)).isTrue();
    assertThat(tbdFoo.containsDirectoryOfTBDForTBD(tbdDepot)).isFalse();

    assertThat(tbdDepot.containsDirectoryOfTBDForTBD(pathAsTargetPattern)).isFalse();
    assertThat(pathAsTargetPattern.containsDirectoryOfTBDForTBD(tbdDepot)).isFalse();

    assertThat(tbdDepot.containsDirectoryOfTBDForTBD(singleTargetPattern)).isFalse();
    assertThat(singleTargetPattern.containsDirectoryOfTBDForTBD(tbdDepot)).isFalse();

    assertThat(tbdDepot.containsDirectoryOfTBDForTBD(targetsInPackagePattern)).isFalse();
    assertThat(targetsInPackagePattern.containsDirectoryOfTBDForTBD(tbdDepot)).isFalse();
  }

  private static TargetPattern parse(String pattern) throws TargetParsingException {
    return TargetPattern.defaultParser().parse(pattern);
  }

  private static TargetPattern parseAsExpectedType(String pattern, Type expectedType)
      throws TargetParsingException {
    TargetPattern parsedPattern = parse(pattern);
    assertThat(parsedPattern.getType()).isEqualTo(expectedType);
    assertThat(parsedPattern.getOriginalPattern()).isEqualTo(pattern);
    return parsedPattern;
  }
}
