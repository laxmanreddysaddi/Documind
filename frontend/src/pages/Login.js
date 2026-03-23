import React, { useState } from "react";
import api from "../api/api";
import { useNavigate } from "react-router-dom";

export default function Login() {

    const [username,setUsername] = useState("");
    const [password,setPassword] = useState("");

    const navigate = useNavigate();

    const login = async () => {

        try{

            const res = await api.post("/auth/login",{
                username,
                password
            });

            localStorage.setItem("token",res.data);

            navigate("/dashboard");

        }catch(err){
            alert("Login failed");
        }

    };

    return(
        <div style={{padding:"40px"}}>
            <h2>Login</h2>

            <input
                placeholder="Username"
                value={username}
                onChange={(e)=>setUsername(e.target.value)}
            />

            <br/><br/>

            <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e)=>setPassword(e.target.value)}
            />

            <br/><br/>

            <button onClick={login}>Login</button>
            <p>
                Don't have an account?
                <button onClick={() => navigate("/register")}>
                    Register
                </button>
            </p>
        </div>
    );

}