An Intelligent Mobile Diary - A Mobile App for Activity Logging and Narrative Generation

Name:VOY
TO-DO:
1. User Login
   - ~~LOGIN page~~
   - ~~FireBase Authentication~~
   - Real email confirmation- check if the domain exists
2. Main Page
   - ~~plus button that lets you add a new diary entry~~
   - ~~display all finished entries~~
   - ~~display message if there are no entries~~
   - ~~user can delete entries(menu button)~~
   - ~~if clicked on an Entry, go to entry page~~
   - ~~MenuBar with Maps(?) and UserAccount dropdown-lets user logout~~
3. Diary Page
   a)NEW ENTRY
   - ~~require Media Permissions, Location~~
   - ~~START button that starts the service~~
   - friendly UI
   - explanation message, steps to follow
   - ~~title required introduced by the user, otherwise untitled trip~~
   b)ENTRY IN THE MAKING
   - ~~all pictures captured by the service appear in the entry page~~
   c)MOCK ENTRY
   - all pictures are stored (and can be deleted by the user?)
   - user can add text - description of the vacation in the beginning of the Entry(optional)
   - button for Narrative Generation
   

# TO DO LIST


- ~~ability to add notes on a trip in the beginning~~ 
- ~~add steps(per day)~~

- ~~make a finished entry look more appealing with space for notes and clustered images if they are close to each other(time) and show location for clusters!!!~~
- ~~user can add tickets manually as pdfs and other media files~~


- ~~create mock trip~~
- ~~fix the location not changing for the emulator~~
- keyboard in login page fix on actual phone
- do something about audio not being captured 
- ~~make play buttons responsive for audio and video~~
- ~~fix audio card~~
- db for more than one device

- button for deleting items instead of just clicking on them
- ~~remove clusters~~
- pdf documents 
- ~~text card for each item~~
- ~~api narrative generation for pictures & video/ audio to text for audio~~
- ~~location service updating every 30 min and then shuts off~~
- date and hour of pictures
- ~~chapters per day in the beginning~~
- summary per day
- ~~days being independent from steps~~
- clean up the app
- ~~save items to app's internal storage so that the AI will always be able to reach them and not be based on android:requestLegacyExternalStorage="true", which is deprecated.~~
- ~~high demand error for gemini fix for audio~~ 

- TO DO list for each trip that is automatically checked off(for landmarks)
- ~~the foreground service should ask for location whenever a new picture is inserted~~ 
- add everything in JSON whenever items are scanned
- gps estimated steps based only on step sensor
- request ignore battery optimizations & workmanager to periodically check if the service is still running

THE STEPS ARE SET TO APPEAR IN THE DIARY AFTER TWO MINUTES FOR TESTING PURPOSES!!!
the service is only location because dataSync kills the service after 6 hours

!! change export schema for the db to true 


### EXTRA INFO

export trip to json:
adb pull /sdcard/Android/data/com.example.voy/files/Voy/ D:\Desktop\Voy

sdcard → Android → data → com.example.voy → files → Voy

adb pull /sdcard/Android/data/com.example.voy/files/Voy/trip_<tripId>.json C:\Users\YourUsername\Desktop\ -- only for a specific one

THE STEPS ARE SET TO APPEAR IN THE DIARY AFTER TWO MINUTES FOR TESTING PURPOSES!!!
the service is only location because dataSync kills the service after 6 hours
the day lasts only two minutes. change in tripForegroundService.

!! change export schema for the db to true 
