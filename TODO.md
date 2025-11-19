App Development (1 @ 60pt)
Create an Android app in Android Studio using Java + XML, with minimum SDK = 29 and target SDK = 35. The app should serve as a simple fitness logger with two activities:
MainActivity
• Displays today's step count (using SensorManager) and counts live
• Displays the five most recent logged activities and their durations
• Clicking a recent activity displays an associated image when clicked
• Includes a button to open AddActivity
AddActivity
• Provides a drop-down menu (Spinner) for activity type (e.g., arms, legs, back, walking, running,
cycling,
• Includes an input field for duration — slider, text entry, clock,
• Has an interactive cornponent that fetches an image from the web using a URL, or pre-
programmed buttons that fetch specific or random images. The images don't impact your grade;
consider adding links to motivational images, memes, emojis, - just nothing NSFW, please
• Presents a save button that stores the activity for later display on MainActivity
• Presents a back button that returns to MainActivity
Each activity nust usable (that is, all elements reachable directly or with scrolling on a range of device
types, SDK versions. and orientations), but are not graded specifically on look and feel.
Some hints for you:
•
•
•
•
•
Consider storing workouts using file storage, SQLite, or SharedPreferences
Make sure to request and handle permissions for step sensor access via SensorManager,
whether in the AndroidManifest.xml, at runtime, or both
Image fetching can take a long time; consider how best to thread this to avoid app not responding
Networks are not always available; handle failures gracefully, e.g. with a timeout and a
placeholder image
Make sure to handle missing sensors, denied permissions, or invalid input (e.g. file extensions)
gracefully
As necessary, save and restore bundles to maintain state across orientation changes
Test on multiple screen sizes and layouts
You can use external libraries as necessary to complete this work
The app will be graded as follows (each line is 6 points; there is no partial credit for incomplete solutions)
•
•
•
•
Project setup: App built in Android Studio using Java + XML; min SDK = 29, target SDK = 35
Permissions: Manifest and runtime permissions correctly requested and handled for sensor access
Step counter: SensorManager step counter implemented, displays live count on MainActivity
Sensor lifecycle management: Step listener registered/unregistered in proper lifecycle methods; no
resource leaks
Activity logging: MainActivity shows up to five most-recent logged activities and handles fewer
gracefully; clicking an activity shows the associated image (either pulled from the web, or cached
locally)
AddActivity input: Drop-down (Spinner) for activity type and duration input field both functional
Data persistence + navigation: Save button stores activity and main list; back navigation
returns cleanly with no duplication or loss
Networking and threading: Image fetched in background thread (e.g. using ExecutorService,
AsyncTask, or some extemal library); no ANR or IJI stall
Error handling: Graceful failure on image or input errors; invalid data and missing permissions handled
without crash
Ul adaptability: Layout responsive to orientation and screen size; state preserved on rotation; works
across SDK versions
If you find yourself overwhelmed, be mindful to target the rubric, not \Et-fection. One of the skills we hope
you leam at MSIJ is how to meet client needs — and your own — while maintaining healthy and
balance.
