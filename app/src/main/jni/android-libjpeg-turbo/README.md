# Warning

# This project along with other ones in [OpenSTF](https://github.com/openstf) organisation is provided as is for community, without active development.
# You can check any other forks that may be actively developed and offer new/different features [here](https://github.com/openstf/stf/network).
# Active development has been moved to [DeviceFarmer](https://github.com/DeviceFarmer) organisation.

# libjpeg-turbo for Android

# Warning

This repository was superseded by https://github.com/DeviceFarmer/android-libjpeg-turbo

This repository provides a working Android.mk build configuration for [libjpeg-turbo](http://libjpeg-turbo.virtualgl.org/), which means that you'll be able to build your app with `ndk-build` instead of resorting to toolchain trickery. Just add this repository as a Git submodule and you should be good to go.

Note however that this repository was originally created for a single purpose; to encode JPG files in a different NDK project. Therefore JNI bindings are not provided but a pull request is welcome. Also, any libjpeg-turbo feature that requires another dependency is currently not supported (incl. colorspace conversions). Again, pull requests are welcome.

Currrently, the following ABIs are supported:

* armeabi
* armeabi-v7a
* armeabi-v7a-hard
* arm64-v8a
* x86
* x86_64, requires [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html) Revision 10e (May 2015) or later

SIMD/ASM optimizations are enabled for supported ABIs where possible. libjpeg-turbo does not support MIPS properly (yet). Since MIPS is not supported, you'll need to specify your supported ABIs in `Application.mk` instead of simply giving it `APP_ABI := all`.

Note that compiler optimization options are NOT included right now. If, for example, you wish to use `-Ofast` or other options, you'll need to add them yourself, possibly to your `Application.mk`.

## Requirements

The following combinations are known to work:

* [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html), Revision 10e (May 2015)

## Usage

First, set up a vendor folder in your own project. You can skip this step if you've already decided where you wish to put the module.

```bash
mkdir -p jni/vendor
echo 'include $(call all-subdir-makefiles)' >> jni/vendor/Android.mk
```

Then add this repo as a submodule to your own project.

```bash
git submodule add --name libjpeg-turbo \
  https://github.com/openstf/android-libjpeg-turbo.git jni/vendor/libjpeg-turbo
```

You should now be able to do `LOCAL_STATIC_LIBRARIES += libjpeg-turbo` in your own module, `#include <turbojpeg.h>` and `ndk-build`.

## Updating libjpeg-turbo

You need to go and fetch [libjpeg-turbo](http://libjpeg-turbo.virtualgl.org/) first. [Download](http://sourceforge.net/projects/libjpeg-turbo/files/) and extract the source package to `jni/vendor/libjpeg-turbo`. It's not a submodule because they still use SVN. Check `jni/vendor/libjpeg-turbo/Android.mk` to see which version of libjpeg-turbo is expected.

```bash
cd jni/vendor/libjpeg-turbo
cp ~/Downloads/libjpeg-turbo-1.4.1.tar.gz .
tar xzvf libjpeg-turbo-1.4.1.tar.gz
```

To make updating easier, the `jni/vendor/libjpeg-turbo/libjpeg-turbo-*-*-*` folder should contain only unmodified original source files. Do not configure or move files around, it will be a pain later. Just unpack the source package and leave it be as-is.

You may sometimes also need to update the convenience files in `jni/vendor/libjpeg-turbo/include`. This doesn't seem to happen very often, though.

Now update the `SOURCE_PATH` in `jni/vendor/libjpeg-turbo/Android.mk`. Then run `ndk-build` to make sure nothing broke.

Then `git rm -r` the previous libjpeg-turbo source and commit the new source with your `Android.mk` modification(s).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

See [LICENSE](LICENSE). libjpeg-turbo itself is governed by its own license.

Copyright © CyberAgent, Inc. All Rights Reserved.
