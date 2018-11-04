# Livebox IN-DEVELOPMENT

Please check https://sserra.gitbook.io/livebox/ for full documentation

# Features

Reactive, support for Observable and LiveData.

- Out of the box caching: memory LRU, disk LRU and disk persistent cache.
- Per request custom data sources, you can use your own data sources to store and retrieve data.
- Uses buffered sources(Okio) every time we read and write data to avoid OOM.
- Pluggable serializers, out of the box support for Gson and Jackson.
- Per request converters, convert data to desired format, before returning it.
- Validators, use your own validators to check local data validity.
  Comes with a built-in AgeValidator that expires local data if it's older than a specified amount of time.
- Share the request. Avoid multiple requests for the same endpoint at the same time, if there's an ongoing request and another one comes in, the second will wait till the first finishes and uses the same response. Saving bandwidth and reducing waiting time.
- Automatic retry on failure. Using interval retry or exponencial backoff retry strategy, customisable per request.
- Lifecycle aware. Support for Uber's auto dispose library, you can pass a LifecycleScopeProvider and it will automatically bound the Observable to passed lifecycle. 

# How to use it

```kotlin

// Builds an instance of Livebox using LiveboxBuilder class.
val usersBox = box<UsersRes, Users>()
                .withKey("users")
                .fetch { api.userList }
                .addSource<UsersRes>(Sources.DISK_LRU, 1.minutes())
                .addSource(UsersRoomDataSource()) { _, users -> users.items.isNotEmpty() }
                .addConverter<UsersRes> { Users.fromUsersRes(it) }
                .retryOnFailure()
                .build()
                .asAndroidObservable()
                   
// Using scoped feature, this uses Uber's autodispose                
usersBox.scoped(AndroidLifecycleScopeProvider.from(this))
                .subscribe(
                   users -> Log.d(TAG, "Users: " + users),
                   Throwable::printStackTrace
                 );
​
```

