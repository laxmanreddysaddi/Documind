import { useState } from "react";
import { login, register } from "../api";

export default function Login({ setUser }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async () => {
    const res = await login(username, password);
    localStorage.setItem("token", res.token);
    localStorage.setItem("username", username);
    setUser(username);
  };

  const handleRegister = async () => {
    await register(username, password);
    alert("Registered successfully!");
  };

  return (
    <div className="h-screen flex items-center justify-center bg-gray-100">
      <div className="p-6 bg-white shadow rounded w-80">
        <h2 className="text-xl font-bold mb-4">DocuMind Login</h2>

        <input className="w-full p-2 border mb-2"
          placeholder="Username"
          onChange={e => setUsername(e.target.value)} />

        <input className="w-full p-2 border mb-2"
          type="password"
          placeholder="Password"
          onChange={e => setPassword(e.target.value)} />

        <button onClick={handleLogin}
          className="w-full bg-blue-500 text-white p-2 mb-2">
          Login
        </button>

        <button onClick={handleRegister}
          className="w-full bg-green-500 text-white p-2">
          Register
        </button>
      </div>
    </div>
  );
}