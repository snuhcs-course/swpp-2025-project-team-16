# SWPP 2025 Project Team 16 Installation Guide

This guide describes how to set up **Pose VLM** and **Django server** on your local environment and the remote server.

---

## 0. Clone the Repository or Download ZIP

You need the project code **both on the remote server and your local machine**:

- **Remote Server:** Django and Pose VLM processes need the code.
- **Local Machine:** Android Studio requires the code to connect to the server and test the app.

**Option 1: Clone via Git**

```bash
git clone --recursive https://github.com/snuhcs-course/swpp-2025-project-team-16.git
git submodule update --init --recursive

```

**Option 2: Download ZIP**

- Download the zip from GitHub and extract it.

**Remote Server Access**

```bash
ssh -p 2204 team16@147.46.78.29

```

- Enter password when prompted.


> ⚠ Make sure the repository exists both remotely and locally before proceeding to environment setup.

---

## 1. Setup Pose VLM Environment (Remote Server)

After connecting to the remote server, go to the Pose VLM directory:

```bash
cd swpp-2025-project-team-16/pose_vlm

```

Create the conda environment:

```bash
conda env create -f environment.yml
conda activate vlm
cd rtmpose3d
python -m pip install -r requirements.txt
python -m pip install -e .

```

> ⚠ Note: The environment name `vlm` specified in `pose_vlm/environment.yml` must match the `POSE_ENV` defined in `server/sportify/settings.py`.

---

## 2. Setup Django Server Environment (Remote Server)

Go to the server directory:

```bash
cd ../server

```

Create the conda environment:

```bash
conda env create -f environment.yml

```

---

## 3. Run the Application

### 3-1. SSH Port Forwarding (Tunneling) from Local Machine

On your **local machine**, run:

```bash
ssh -L 8004:localhost:8080 -p 2204 team16@147.46.78.29

```

- This forwards remote port `8080` to your local port `8004`.
- Enter password when prompted.

---

### 3-2. Start Django Server (Remote Server)

In a separate terminal:  

```bash
ssh -p 2204 team16@147.46.78.29
cd swpp-2025-project-team-16/server
conda activate django_server_env
python manage.py runserver 0.0.0.0:8080

```

- The server now listens on port `8080` on the remote server
- Accessible locally via `localhost:8004` through SSH tunnel
- `django_server_env` is the environment name specified in `server/environment.yml`.

---

### 3-3. Launch Android Studio App (Local Machine)

- Open the Android Studio project with the local repository
- Connect your smartphone via USB.
- Run the app on your device.
- The app will communicate with the server through the forwarded port `8004`.
