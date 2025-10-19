## Iteration 2 Updates

### User Features
#### Registration & Authentication
- Signup and login implemented.  
- Password and email verification in place.  
- Login currently uses a temporary MySQL database; validation fails if credentials do not match.
#### Database
- DB schema is under consideration; full integration not yet completed.

---

### Recommendation Features
#### OpenAI Integration
- Recommendations are generated via OpenAI API; real products are not stored in the database.  
- Transition from embedding model (Iteration 1) to OpenAI API (Iteration 2).
#### API Integration
- Not completed — API key not yet received.
#### Dummy Implementation
- Verified JSON handling and app integration.
#### Notes
- Recommendation logic is not fully implemented yet, but the app can process and display dummy results.

---

### Pose Features
#### MediaPipe Model Integration
- Pose detection model integrated and functional in the app.
#### Changes
- Several updates applied; details of modifications are not fully documented.

---

### Schedule Features
#### Schedule Creation
- Schedule creation logic added

---

### Demo Video
- link 1: `https://drive.google.com/file/d/1yLaxindcJVp0D3gl3Qdqq4R4G-T1-f8o/view?usp=drive_link`
- link 2: `https://drive.google.com/file/d/1yLaxindcJVp0D3gl3Qdqq4R4G-T1-f8o/view?usp=drive_link`

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
