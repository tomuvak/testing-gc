# `com.tomuvak.testing-gc` – a multi-platform Kotlin library with utilities for tests relying on garbage collection
This library is licensed under the [MIT License](https://en.wikipedia.org/wiki/MIT_License);
see [LICENSE.txt](LICENSE.txt).

## Table of contents
* [Rationale](#rationale)
  * [General](#general)
  * [Vis-à-vis `com.tomuvak.testing-gc-core`](#vis-à-vis-comtomuvaktesting-gc-core)
* [Including this library in a Kotlin project](#including-this-library-in-a-kotlin-project)
* [Caveats](#caveats)
* [Adjusting timeout (JS)](#adjusting-timeout-js)

## Rationale

### General
Part of the correctness of code lies within its releasing of resources once it no longer needs them. And part of these
resources is memory allocated for objects. When writing in a language with automatic
[garbage collection](https://en.wikipedia.org/wiki/Garbage_collection_(computer_science)) it is not normally the
responsibility of the programmer to actually make sure memory which is no longer referenced anywhere in the program
gets freed. But – and this is too often overlooked – **it _is_ their responsibility to make sure _objects which are no
longer needed are no longer referenced_**. Subtle bugs where this responsibility is neglected abound ([here's an
example](https://github.com/JetBrains/kotlin/commit/c0cac21b8a3170b2d6ec1e077562a78e557f0b5f#r78800018) from Kotlin's
standard library).

Being aware of this issue a programmer can try and write correct code. But that's not always enough: ideally the
correctness of the code should also be verified by tests – both to make sure that [future changes don't accidentally
spoil it](https://en.wikipedia.org/wiki/Software_regression), and to make sure that what seems to be correct actually
does work as expected ([two](https://stackoverflow.com/a/71537602) [examples](https://stackoverflow.com/a/73070221)
where releasing memory in Kotlin might not work as expected).

Unfortunately, it seems that most software developers aren't too aware of or concerned with the issue, and also, perhaps
relatedly, that there isn't great tooling to tackle the problem. In addition, because of the nature of the systems
involved, whatever tooling does exist naturally tends to be platform-specific.

This library tries to offer some remedy, by providing utilities which (at least to some extent) do enable testing for
the releasing of memory and (at least to some extent) are exposed with a unified interface that allows users of the
library to write a single version of their tests which can then run cross-platform.

### Vis-à-vis [`com.tomuvak.testing-gc-core`][1]
The sister library [`com.tomuvak.testing-gc-core`][1] contains _primitive_ utilities, which can be used to trigger
garbage collection, but no mechanism on top of that to help verify any desired effects of garbage collection have
actually taken place.

This library, on the other hand, offers slightly higher-level constructs, to help work with 
[weak references](https://en.wikipedia.org/wiki/Weak_reference) (provided by
[`com.tomuvak.weak-reference`](https://github.com/tomuvak/weak-reference)) and verify that their targets are no longer
being held by components under test.

## Including this library in a Kotlin project
To add the library from
[GitHub Packages](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages), a
reference to this repository's GitHub Packages
[Maven repository](https://maven.apache.org/guides/introduction/introduction-to-repositories.html) needs to be added
inside the `repositories { ... }` block in the project's `build.gradle.kts` file:

```kotlin
    maven {
        url = uri("https://maven.pkg.github.com/tomuvak/testing-gc")
        credentials { // See note below
            username = "<GitHub user name>"
            password = "<GitHub personal access token>"
        }
    }
```

and the dependency should be declared for the relevant source set(s) inside the relevant `dependencies { ... }` block(s)
inside the `sourceSet { ... }` block, e.g.

```kotlin
        val commonTest by getting {
            dependencies {
                implementation("com.tomuvak.testing-gc:testing-gc:<version>")
            }
        }
```

([![Latest release version][v]](https://github.com/tomuvak/testing-gc/tags)) to add it for the test source sets on
all platforms in a multi-platform project.

Note about credentials: it seems that even though this repository is public and everyone can download this library from
GitHub Packages, one still needs to supply credentials for some reason. Any GitHub user should work, when provided with
a [personal access
token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)
for the user with (at least) the `read:packages` scope.

**You might want to keep the credentials private**, for example in case the GitHub user has access to private packages
(as GitHub personal access tokens can be restricted in the type of operations they're used for, but not in the
repositories they can access), or all the more so in case the token has a wider scope (and note also that one can change
a token's scope after its creation, so it's possible that at some future point the user might inadvertently grant a
token which was meant to be restricted more rights).

See this library's own [Gradle script](build.gradle.kts) for an example of one way this could be done by means of
storing private information in a local file which is not source-controlled. In this case the file – which is Git-ignored
– is called `local.properties`, and it includes lines like the following:

```properties
githubUser=<user name>
githubToken=<personal access token for the user above, with the `read:packages` scope>
```

## Caveats
* The functionality provided – while proven to have worked for some projects under some circumstances – cannot and does
  not guarantee that attempting to trigger garbage collection will indeed always trigger garbage collection.
* As in the [two](https://stackoverflow.com/a/71537602) [examples](https://stackoverflow.com/a/73070221) mentioned
  above, it's possible there'll be hidden references preventing objects from being reclaimed. Diagnosing such cases
  might not be trivial. Extracting the generation of the objects which are to be reclaimed to other functions (this is
  part of the motivation for the function `generateSequenceAndWeakReferences`).
* While [`com.tomuvak.testing-gc-core`][1] provides (also) a way to trigger garbage collection directly and
  synchronously _on platforms which support that_ (notably excluding JS), and while some of the functionality provided
  by this library can be combined with that for code which only targets such platforms, the main functionality this
  library provides uses `testing-gc-core` functionality which works (or strives to work) cross-platform, but is
  _potentially asynchronous_, and can only be called from a coroutine. That means that code using it must be written
  accordingly, e.g. by using [`kotlinx.coroutines.runBlocking`][2] (not supported on JS) or some other mechanism (the
  sister library [`com.tomuvak.testing-coroutines`](https://github.com/tomuvak/testing-coroutines) provides the
  [`asyncTest`][3] function for that).
* Where garbage collection cannot be triggered directly (= on JS), the mechanism which tries to trigger it uses a
  memory-intensive computation, which might also take longer time to run than a typical test might be expected to take.
  This means longer times for running tests should be taken into account, and also that with default configuration some
  tests might fail because of timeout violation (see [next section](#adjusting-timeout-js) for information about
  adjusting timeout for tests on JS).

## Adjusting timeout (JS)
On Kotlin JS, the implementation of assertions which rely on garbage collection tries to trigger garbage collection by
performing heavy computations. As such, tests using it might require a long time to run, and with default configuration
will possibly fail because they exceed the default timeout.

To change the timeout assign the desired value (e.g. `"12s"` – that is twelve seconds – suffices for known usages, but
the exact time needed for a specific test might vary greatly depending on the specifics of the test and of the
environment on which it is run) to `timeout` within the `useMocha` block within the `testTask` block within the
`browser`/`nodejs` block within the `js` block within the `kotlin` block in the `build.gradle.kts` file (creating the
relevant blocks if they don't already exist), e.g.:

```kotlin
.
.
.

kotlin {
    .
    .
    .

    js(BOTH) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
            testTask {
                useMocha {
                    timeout = "12s"
                }
            }
        }
    }

    .
    .
    .
}

.
.
.
```

[v]: https://img.shields.io/github/v/tag/tomuvak/testing-gc?label=Latest%20release%20version%20%28ignoring%20the%20initial%20%27v%27%29%3A&style=plastic
[1]: https://github.com/tomuvak/testing-gc-core
[2]: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html
[3]: https://github.com/tomuvak/testing-coroutines#using-the-functionality-in-code
