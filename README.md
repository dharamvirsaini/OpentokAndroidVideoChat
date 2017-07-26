# OpentokAndroidVideoChat

I have created a sample android app much like WhatsApp but it is completely based on Tokbox platform and is audio/video only. In past, we’ve received a number of queries regarding integration of user authentication, one-one/one-many calling using push notification etc. So, this app collectively integrated all these use-cases into one and present an abstract UI to the end user. Following are the main highlights of this app:

1) User authentication using Phone number verification(Firebase SDK)
2) Syncing phone contacts with the app database(Phone contacts who are using the app are only shown in the app)
3) Push notifications to show incoming call alerts(Using Firebase Push Notification services)
4) Profile image and name is synced with database.

I have used Glide open source library because of the following:

a) Handling ImageView recycling and cancelation in an adapter
b) Complex image transformations with minimal memory use. 

c) Automatic memory and disk caching.

Additional points covered are: 

a). Based on MVC architecture 

b). Non UI blocking Code 

c). Image Caching

d). Memory Management
