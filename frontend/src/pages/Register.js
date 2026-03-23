import React, { useState } from "react";
import api from "../api/api";
import { useNavigate } from "react-router-dom";

export default function Register() {

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    const navigate = useNavigate();

    const register = async () => {

        try {

            await api.post("/auth/register", {
                username,
                password
            });

            alert("Registration successful");

            navigate("/");

        } catch (error) {

            alert("Registration failed");

        }

    };

    return (

        <div style={{ padding: "40px" }}>

            <h2>Register</h2>

            <input
                placeholder="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
            />

            <br /><br />

            <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
            />

            <br /><br />

            <button onClick={register}>
                Register
            </button>

            <br /><br />

            <p>
                Already have an account?
                <button onClick={() => navigate("/")}>
                    Login
                </button>
            </p>

        </div>

    );
}