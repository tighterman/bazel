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

package com.google.devtools.build.lib.rules.apple;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration.DefaultLabelConverter;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration.LabelConverter;
import com.google.devtools.build.lib.analysis.config.FragmentOptions;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.rules.apple.AppleConfiguration.ConfigurationDistinguisher;
import com.google.devtools.build.lib.rules.apple.ApplePlatform.PlatformType;
import com.google.devtools.build.lib.skylarkinterface.SkylarkModule;
import com.google.devtools.build.lib.skylarkinterface.SkylarkModuleCategory;
import com.google.devtools.common.options.Converters.CommaSeparatedOptionListConverter;
import com.google.devtools.common.options.EnumConverter;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionsParser.OptionUsageRestrictions;
import com.google.devtools.common.options.proto.OptionFilters.OptionEffectTag;
import java.util.List;

/**
 * Command-line options for building for Apple platforms.
 */
public class AppleCommandLineOptions extends FragmentOptions {

  @Option(
    name = "xcode_version",
    defaultValue = "null",
    category = "build",
    converter = DottedVersionConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help =
        "If specified, uses Xcode of the given version for relevant build actions. "
            + "If unspecified, uses the executor default version of Xcode."
  )
  // TODO(bazel-team): This should be of String type, to allow referencing an alias based
  // on an xcode_config target.
  public DottedVersion xcodeVersion;

  @Option(
    name = "ios_sdk_version",
    defaultValue = "null",
    converter = DottedVersionConverter.class,
    category = "build",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Specifies the version of the iOS SDK to use to build iOS applications."
  )
  public DottedVersion iosSdkVersion;

  @Option(
    name = "watchos_sdk_version",
    defaultValue = "null",
    converter = DottedVersionConverter.class,
    category = "build",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Specifies the version of the watchOS SDK to use to build watchOS applications."
  )
  public DottedVersion watchOsSdkVersion;

  @Option(
    name = "tvos_sdk_version",
    defaultValue = "null",
    converter = DottedVersionConverter.class,
    category = "build",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Specifies the version of the tvOS SDK to use to build tvOS applications."
  )
  public DottedVersion tvOsSdkVersion;

  @Option(
    name = "macos_sdk_version",
    defaultValue = "null",
    converter = DottedVersionConverter.class,
    category = "build",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Specifies the version of the macOS SDK to use to build macOS applications."
  )
  public DottedVersion macOsSdkVersion;

  @Option(
    name = "ios_minimum_os",
    defaultValue = "null",
    category = "flags",
    converter = DottedVersionConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Minimum compatible iOS version for target simulators and devices."
  )
  public DottedVersion iosMinimumOs;

  @Option(
    name = "watchos_minimum_os",
    defaultValue = "null",
    category = "flags",
    converter = DottedVersionConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Minimum compatible watchOS version for target simulators and devices."
  )
  public DottedVersion watchosMinimumOs;

  @Option(
    name = "tvos_minimum_os",
    defaultValue = "null",
    category = "flags",
    converter = DottedVersionConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Minimum compatible tvOS version for target simulators and devices."
  )
  public DottedVersion tvosMinimumOs;

  @Option(
    name = "macos_minimum_os",
    defaultValue = "null",
    category = "flags",
    converter = DottedVersionConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Minimum compatible macOS version for targets."
  )
  public DottedVersion macosMinimumOs;

  @VisibleForTesting public static final String DEFAULT_IOS_SDK_VERSION = "8.4";
  @VisibleForTesting public static final String DEFAULT_WATCHOS_SDK_VERSION = "2.0";
  @VisibleForTesting public static final String DEFAULT_MACOS_SDK_VERSION = "10.10";
  @VisibleForTesting public static final String DEFAULT_TVOS_SDK_VERSION = "9.0";
  @VisibleForTesting static final String DEFAULT_IOS_CPU = "x86_64";

  /**
   * The default watchos CPU value.
   */
  public static final String DEFAULT_WATCHOS_CPU = "i386";

  /**
   * The default tvOS CPU value.
   */
  public static final String DEFAULT_TVOS_CPU = "x86_64";

  /**
   * The default macOS CPU value.
   */
  public static final String DEFAULT_MACOS_CPU = "x86_64";

  @Option(
    name = "ios_cpu",
    defaultValue = DEFAULT_IOS_CPU,
    category = "build",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Specifies to target CPU of iOS compilation."
  )
  public String iosCpu;

  @Option(
    name = "apple_crosstool_top",
    defaultValue = "@bazel_tools//tools/cpp:toolchain",
    category = "version",
    converter = LabelConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help =
        "The label of the crosstool package to be used in Apple and Objc rules and their"
            + " dependencies."
  )
  public Label appleCrosstoolTop;

  @Option(
    name = "apple_platform_type",
    defaultValue = "IOS",
    optionUsageRestrictions = OptionUsageRestrictions.UNDOCUMENTED,
    converter = PlatformTypeConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help =
        "Don't set this value from the command line - it is derived from other flags and "
            + "configuration transitions derived from rule attributes"
  )
  public PlatformType applePlatformType;

  @Option(
    name = "apple_split_cpu",
    defaultValue = "",
    optionUsageRestrictions = OptionUsageRestrictions.UNDOCUMENTED,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help =
        "Don't set this value from the command line - it is derived from other flags and "
            + "configuration transitions derived from rule attributes"
  )
  public String appleSplitCpu;

  // This option exists because two configurations are not allowed to have the same cache key
  // (partially derived from options). Since we have multiple transitions that may result in the
  // same configuration values at runtime we need an artificial way to distinguish between them.
  // This option must only be set by those transitions for this purpose.
  // TODO(bazel-team): Remove this once we have dynamic configurations but make sure that different
  // configurations (e.g. by min os version) always use different output paths.
  @Option(
    name = "apple configuration distinguisher",
    defaultValue = "UNKNOWN",
    converter = ConfigurationDistinguisherConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    optionUsageRestrictions = OptionUsageRestrictions.INTERNAL
  )
  public ConfigurationDistinguisher configurationDistinguisher;

  @Option(
    name = "ios_multi_cpus",
    converter = CommaSeparatedOptionListConverter.class,
    defaultValue = "",
    category = "flags",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help =
        "Comma-separated list of architectures to build an ios_application with. The result "
            + "is a universal binary containing all specified architectures."
  )
  public List<String> iosMultiCpus;

  @Option(
    name = "watchos_cpus",
    converter = CommaSeparatedOptionListConverter.class,
    defaultValue = DEFAULT_WATCHOS_CPU,
    category = "flags",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Comma-separated list of architectures for which to build Apple watchOS binaries."
  )
  public List<String> watchosCpus;

  @Option(
    name = "tvos_cpus",
    converter = CommaSeparatedOptionListConverter.class,
    defaultValue = DEFAULT_TVOS_CPU,
    category = "flags",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Comma-separated list of architectures for which to build Apple tvOS binaries."
  )
  public List<String> tvosCpus;

  @Option(
    name = "macos_cpus",
    converter = CommaSeparatedOptionListConverter.class,
    defaultValue = DEFAULT_MACOS_CPU,
    category = "flags",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "Comma-separated list of architectures for which to build Apple macOS binaries."
  )
  public List<String> macosCpus;

  @Option(
    name = "default_ios_provisioning_profile",
    defaultValue = "",
    optionUsageRestrictions = OptionUsageRestrictions.UNDOCUMENTED,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    converter = DefaultProvisioningProfileConverter.class
  )
  public Label defaultProvisioningProfile;

  @Option(
    name = "xcode_version_config",
    defaultValue = "@local_config_xcode//:host_xcodes",
    optionUsageRestrictions = OptionUsageRestrictions.UNDOCUMENTED,
    converter = LabelConverter.class,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help =
        "The label of the xcode_config rule to be used for selecting the Xcode version "
            + "in the build configuration."
  )
  public Label xcodeVersionConfig;

  /**
   * The default label of the build-wide {@code xcode_config} configuration rule. This can be
   * changed from the default using the {@code xcode_version_config} build flag.
   */
  // TODO(cparsons): Update all callers to reference the actual xcode_version_config flag value.
  static final String DEFAULT_XCODE_VERSION_CONFIG_LABEL = "//tools/objc:host_xcodes";

  /** Converter for --default_ios_provisioning_profile. */
  public static class DefaultProvisioningProfileConverter extends DefaultLabelConverter {
    public DefaultProvisioningProfileConverter() {
      super("//tools/objc:default_provisioning_profile");
    }
  }

  @Option(
    name = "xcode_toolchain",
    defaultValue = "null",
    category = "flags",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help =
        "The identifier of an Xcode toolchain to use for builds. Currently only the toolchains "
            + "that ship with Xcode are supported. For example, in addition to the default "
            + "toolchain Xcode 8 has 'com.apple.dt.toolchain.Swift_2_3' which can be used for "
            + "building legacy Swift code."
  )
  public String xcodeToolchain;

  @Option(
    name = "apple_bitcode",
    converter = AppleBitcodeMode.Converter.class,
    // TODO(blaze-team): Default to embedded_markers when fully implemented.
    defaultValue = "none",
    category = "flags",
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help =
        "Specify the Apple bitcode mode for compile steps. "
            + "Values: 'none', 'embedded_markers', 'embedded'."
  )
  public AppleBitcodeMode appleBitcodeMode;

  @Option(
    name = "apple_crosstool_transition",
    defaultValue = "true",
    optionUsageRestrictions = OptionUsageRestrictions.UNDOCUMENTED,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "If true, the apple crosstool is used for all apple rules."
  )
  public boolean enableAppleCrosstoolTransition;

  @Option(
    name = "target_uses_apple_crosstool",
    defaultValue = "false",
    optionUsageRestrictions = OptionUsageRestrictions.UNDOCUMENTED,
    documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
    effectTags = {OptionEffectTag.UNKNOWN},
    help = "If true, this target uses the apple crosstool.  Do not set this flag manually."
  )
  public boolean targetUsesAppleCrosstool;

  private ApplePlatform getPlatform() {
    for (String architecture : iosMultiCpus) {
      if (ApplePlatform.forTarget(PlatformType.IOS, architecture) == ApplePlatform.IOS_DEVICE) {
        return ApplePlatform.IOS_DEVICE;
      }
    }
    return ApplePlatform.forTarget(PlatformType.IOS, iosCpu);
  }

  /**
   * Returns the architecture implied by these options.
   *
   * <p> In contexts in which a configuration instance is present, prefer
   * {@link AppleConfiguration#getSingleArchitecture}.
   */
  public String getSingleArchitecture() {
    if (!Strings.isNullOrEmpty(appleSplitCpu)) {
      return appleSplitCpu;
    }
    switch (applePlatformType) {
      case IOS:
        if (!iosMultiCpus.isEmpty()) {
          return iosMultiCpus.get(0);
        } else {
          return iosCpu;
        }
      case WATCHOS:
        return watchosCpus.get(0);
      case TVOS:
        return tvosCpus.get(0);
      case MACOS:
        return macosCpus.get(0);
      default:
        throw new IllegalArgumentException("Unhandled platform type " + applePlatformType);
    }
  }

  /**
   * Represents the Apple Bitcode mode for compilation steps.
   *
   * <p>Bitcode is an intermediate representation of a compiled program. For many platforms, Apple
   * requires app submissions to contain bitcode in order to be uploaded to the app store.
   *
   * <p>This is a build-wide value, as bitcode mode needs to be consistent among a target and its
   * compiled dependencies.
   */
  @SkylarkModule(
    name = "apple_bitcode_mode",
    category = SkylarkModuleCategory.NONE,
    doc =
        "Apple Bitcode mode for compilation steps. Possible values are \"none\", "
            + "\"embedded\", and \"embedded_markers\""
  )
  @Immutable
  public enum AppleBitcodeMode {

    /** Do not compile bitcode. */
    NONE("none", ImmutableList.<String>of()),
    /**
     * Compile the minimal set of bitcode markers. This is often the best option for developer/debug
     * builds.
     */
    EMBEDDED_MARKERS(
        "embedded_markers", ImmutableList.of("bitcode_embedded_markers"), "-fembed-bitcode-marker"),
    /** Fully embed bitcode in compiled files. This is often the best option for release builds. */
    EMBEDDED("embedded", ImmutableList.of("bitcode_embedded"), "-fembed-bitcode");

    private final String mode;
    private final ImmutableList<String> featureNames;
    private final ImmutableList<String> clangFlags;

    private AppleBitcodeMode(
        String mode, ImmutableList<String> featureNames, String... clangFlags) {
      this.mode = mode;
      this.featureNames = featureNames;
      this.clangFlags = ImmutableList.copyOf(clangFlags);
    }

    @Override
    public String toString() {
      return mode;
    }

    /** Returns the names of any crosstool features that correspond to this bitcode mode. */
    public ImmutableList<String> getFeatureNames() {
      return featureNames;
    }

    /**
     * Returns the flags that should be added to compile and link actions to use this
     * bitcode setting.
     */
    public ImmutableList<String> getCompileAndLinkFlags() {
      return clangFlags;
    }

    /**
     * Converts to {@link AppleBitcodeMode}.
     */
    public static class Converter extends EnumConverter<AppleBitcodeMode> {
      public Converter() {
        super(AppleBitcodeMode.class, "apple bitcode mode");
      }
    }
  }

  @Override
  public FragmentOptions getHost(boolean fallback) {
    AppleCommandLineOptions host = (AppleCommandLineOptions) super.getHost(fallback);

    // Set options needed in the host configuration.
    host.xcodeVersionConfig = xcodeVersionConfig;
    host.xcodeVersion = xcodeVersion;
    host.iosSdkVersion = iosSdkVersion;
    host.watchOsSdkVersion = watchOsSdkVersion;
    host.tvOsSdkVersion = tvOsSdkVersion;
    host.macOsSdkVersion = macOsSdkVersion;
    host.appleBitcodeMode = appleBitcodeMode;
    // The host apple platform type will always be MACOS, as no other apple platform type can
    // currently execute build actions. If that were the case, a host_apple_platform_type flag might
    // be needed. 
    host.applePlatformType = PlatformType.MACOS;

    return host;
  }

  /** Converter for the Apple configuration distinguisher. */
  public static final class ConfigurationDistinguisherConverter
      extends EnumConverter<ConfigurationDistinguisher> {
    public ConfigurationDistinguisherConverter() {
      super(ConfigurationDistinguisher.class, "Apple rule configuration distinguisher");
    }
  }

  /** Flag converter for {@link PlatformType}. */
  public static final class PlatformTypeConverter
      extends EnumConverter<PlatformType> {
    public PlatformTypeConverter() {
      super(PlatformType.class, "Apple platform type");
    }
  }
}
