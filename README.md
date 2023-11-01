# AR-navigation
AR-navigation is a mobile application developed for the purpose of completing a university assignment, however, I will be building on top of it starting June 2023.
A map application that allows you to use your camera to see different points of interest using OpenGL to build AR environment. I use Kotlin as the main language
however, I have some Java classes for the conversions that happen within my application. 

This is nowhere near a final product, as there is a lot to be worked on. However, the main functionalities seem to be working, but I have to solve some major bugs regarding the notification system, and the Cube renderer, as it doesn't render the colors of the cubes.

It uses broadcasting the user's location, notifications, adapters and ViewModels, alongside a repository to easily manage data coming from different sources.

#More Updates to come



## An example of how the app currently displays a point of interest.
![image](https://github.com/ToniAnton22/AR-navigation/assets/72076515/b6fd6fb9-340e-423f-9d53-a3bc1acc00b9)


## Getting Started

To run the app in your IDE, you will need Android Studio Giraffe 2022 (last IDE tried on)
- Set up a phone emulator (With API between 33 and 29)
- Run the app and make sure to give it permission to use your camera and location, as the app cannot function otherwise

## How to use it

The start-up page is the AR environment. This is used to find the places near you, and your camera will have an overlay objects in forms of black cubes when it detect a point of interest close to your location
you can access the menu by dragging it from the right side of the screen. There you can switch to the "map" activity, that has a list of points of interests found in an area of about 2-3 miles away from you, and it will show them on the map fragment below the list.
Lastly, you can change what points of interest show up based on their category, by selecting the top right menu button, and setting your preferences. The "go there" button currently does nothing.
