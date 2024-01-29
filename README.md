# Getting Started

Before you begin changing this template project, start by updating the project name.
Do so by updating the setting assignment found in the `settings.gradle` file:
```
rootProject.name = 'personalization-plugin-starter'
```
to match the name of the github repository where this project is hosted.

### Requirements
Your `github` username and a personal access token (https://github.com/settings/tokens) must be either in the environment variables `GITHUB_READPACKAGES_USER` and `GITHUB_READPACKAGES_TOKEN`, **or** in the properties `githubReadPackagesUser` and `githubReadPackagesToken` in `<project-root>/local.properties`.

Ensure you have the Android build tools version `30.0.3` installed, and that either your `ANDROID_HOME` environment variable **or** the property `sdk.dir` points to your Android SDK install location

Example `local.properties` file content:
  ```
  sdk.dir=/home/researcher/Android/Sdk
  githubReadPackagesToken=g12_345678909876543234567890hhh123456
  githubReadPackagesUser=eidudev
  ```

### Where to implement your personalization logic
The EIDU app expects your plugin to implement the `PersonalizationPlugin` interface. All of the logic should be executed within the function `PersonalizationPlugin.determineNextUnits()`. You are of course free to organize your code as you wish (i.e. in other classes or files) - but in the end this one function is both the entrance and exit point for your plugin. 

We have prepared a starter implementation for you: `PluginImplementation`. You can rename this class, but if you do you must also change the property `pluginClass` in `build.gradle.kts` accordingly.

### How to publish
To publish, you must first configure two gitub secrets for the action: `ACCOUNT_NAME` and `ACCOUNT_PASSWORD`. These will be provided to you by EIDU.

Push your changes and then push a version tag to main. Doing so will trigger a GitHub `publish` workflow that will notify EIDU of the plugin release candidate.
Version tags take the format `v-*.*.*`. 

Example tag push:
```
git tag v-0.0.1
git push origin v-0.0.1
```
For more information on tags, visit [Git - Tagging](https://git-scm.com/book/en/v2/Git-Basics-Tagging).
