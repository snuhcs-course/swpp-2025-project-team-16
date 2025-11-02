## Iteration 3 Updates

### User Features
#### Registration & Authentication with new UI
- Signup and login implemented.  
- Password and email verification in place.  
- Login currently uses a temporary MySQL database; validation fails if credentials do not match.
#### Database
- DB schema is well-planned, but yet not implemented.

---

### Pose Features
#### MediaPipe Model Integration with new UI
- Pose detection model integrated and functional in the app.
#### Feedback Functionality Implementation
- uses VLM with image which is taken in app. 
#### Changes
- Several updates applied; details of modifications are not fully documented.

---

### Schedule Features
#### Schedule Creation with new UI
- Schedule creation logic added
  yet fully integrated with other parts

---

### Demo Video
- link: https://drive.google.com/file/d/1yTbT5iAS6IawYoWdvj64U8ftuFSwOu17/view?usp=drive_link

---

### Install
```
django
djangorestframework
openai
mysqlclient
python-dotenv
PyJWT
```

1. get access authority  
```
ssh -L 3306:localhost:3306 -p 2204 team16@147.46.78.29
```

2. activate conda environment and run django server with `0.0.0.0:8000`  
3. if you want to connect your phone, please change `BASE_URL` with your wifi environment in `RetrofitClient.kt`
   and add that IP address in ALLOWED_HOSTS in `settings.py`  
