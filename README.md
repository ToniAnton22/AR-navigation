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
