import { useState, useEffect } from "react";
import { login, register } from "../api";

export default function Login({ setUser }) {

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);

  // 🔥 Wake backend (important for speed)
  useEffect(() => {
    fetch("https://documind-backend-30m4.onrender.com/api/chat?question=hi&username=test")
      .catch(() => {});
  }, []);

  // ✅ LOGIN
  const handleLogin = async () => {
    if (!username || !password) {
      alert("Enter username & password");
      return;
    }

    try {
      setLoading(true);

      const res = await login(username, password);

      if (!res.token) {
        alert("Invalid credentials");
        return;
      }

      // ✅ Save session
      localStorage.setItem("token", res.token);
      localStorage.setItem("username", username);

      setUser(username);

    } catch (err) {
      alert("❌ " + err.message);
    } finally {
      setLoading(false);
    }
  };

  // ✅ REGISTER
  const handleRegister = async () => {
    if (!username || !password) {
      alert("Enter username & password");
      return;
    }

    try {
      setLoading(true);

      await register(username, password);

      alert("✅ Registered! Now login");
      setIsLogin(true);

    } catch (err) {
      alert("❌ " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-screen flex items-center justify-center bg-gray-100">

      <div className="bg-white p-8 rounded-xl shadow-xl w-96">

        <h2 className="text-2xl font-bold mb-4 text-center">
          DocuMind AI
        </h2>

        <input
          className="w-full border p-2 mb-3 rounded"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />

        <input
          type="password"
          className="w-full border p-2 mb-3 rounded"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {/* 🔥 LOGIN BUTTON */}
        {isLogin ? (
          <button
            onClick={handleLogin}
            disabled={loading}
            className={`w-full p-2 rounded text-white ${
              loading ? "bg-gray-400" : "bg-blue-500"
            }`}
          >
            {loading ? "Logging in..." : "Login"}
          </button>
        ) : (
          <button
            onClick={handleRegister}
            disabled={loading}
            className={`w-full p-2 rounded text-white ${
              loading ? "bg-gray-400" : "bg-green-500"
            }`}
          >
            {loading ? "Registering..." : "Register"}
          </button>
        )}

        {/* 🔁 SWITCH */}
        <p
          className="mt-4 text-blue-600 text-center cursor-pointer"
          onClick={() => setIsLogin(!isLogin)}
        >
          {isLogin
            ? "Don't have account? Register"
            : "Already have account? Login"}
        </p>

      </div>
    </div>
  );
}