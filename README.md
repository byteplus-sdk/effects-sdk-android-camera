# Introduction #
A simple camera app implementing Camera2 API and BytePlus Effects Java SDK to showcase the most basic integration in Android, with some example effects included:

* a filter
* a sunglass AR try-on
* a makeup sticker (lipstick)

The repository is meant for Android developers to reference and learn how to start with BytePlus Effect SDK integration quickly.

# Code Structure #
The integration source code can be found at:

    app/src/main/java/com/bytedance/labcv/demo 

The key source code to look at are:

* `MainActivity.java`: a standard Android activity class, with the unzip task implemented to copy and unzip the effect assets provided by BytePlus team
* `CameraUtil.java`: a Camera2 API implemetation with utility functions for operating camera
* `CameraFragment.java`: the implementation of openGL ES environment with a `GLSurfaceView` and Effect Manager can be found in this file

In addition, you'd need to request for the following resources at BytePlus team to include into the project and start testing:

* an Effect SDK trial license bag, to put at `app/src/main/assets/resource/`
* the SDK library (an `AAR` binary), to put at `app/libs/`
* the Effect `core` folder containing helper functions, to put at `app/src/main/java/com/bytedance/labcv/`
* the Effect bundle resource assets, to put at `app/src/main/assets/resource/`

# Getting Started #
Follow the steps below in order to run and test this demo project:
<ol>
    <li>Clone this git repository</li>
    <li>Share your bundle ID with BytePlus team, and request for the additional resource at BytePlus as explained in section above</li>
    <li>Put the additional resource in the path as required</li>
    <li>Change the project's bundle ID to yours</li>
    <li>Start running or debugging the project!</li>
</ol>

# Disclaimer #
In order to minimize the code size and make the integration code easy to follow, we've made the demo project simple and there are limitations for you to know:

- coverage on low-end Android devices using Camera v1 API (deprecated version)
- UI/UX is simple and functional for demo only
- Some edge case error handling may not be included yet

Yet we'd like to clarify that the above are limitations of how the project is implemented, NOT the limitations from the Effect SDK.

If you'd like to see a full-fledged demo, please request for such at BytePlus team too.

# Contact #

- 