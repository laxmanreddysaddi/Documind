import { useState, useEffect, useRef } from "react";
import axios from "axios"
 const BASE_URL = "https://documind-backend-30m4.onrender.com";;

export default function App() {

  // 🔐 AUTH
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState("");

  // 💬 CHAT
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);

  const textareaRef = useRef(null);
  const chatEndRef = useRef(null);

  // 🚀 CLEAR INPUTS
  useEffect(() => {
    setUsername("");
    setPassword("");
  }, []);

  // 🔽 Auto scroll
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  // 🔄 Auto resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height =
        textareaRef.current.scrollHeight + "px";
    }
  }, [question]);

  // 🔐 AUTH
  const handleAuth = async () => {
    try {
      const url = isLogin
        ? `${BASE_URL}/api/auth/login`
        : `${BASE_URL}/api/auth/register`;

      const res = await axios.post(url, { username, password });

      if (isLogin) {
        setToken(res.data);
        console.log("✅ Token:", res.data);
      } else {
        alert("Registered! Now login");
        setIsLogin(true);
      }
    } catch (err) {
      alert("❌ Auth Error: " + (err.response?.data || err.message));
    }
  };

  // 💬 SEND MESSAGE
  const sendMessage = async () => {
    if (!question.trim()) return;

    const userMsg = { role: "user", text: question };
    setMessages((prev) => [...prev, userMsg]);
    setQuestion("");
    setLoading(true);

    try {
      const res = await axios.get(
        `${BASE_URL}/api/chat?question=${question}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      const fullText = res.data;

      let currentText = "";
      let i = 0;

      const interval = setInterval(() => {
        currentText += fullText[i];
        i++;

        setMessages((prev) => {
          const updated = [...prev];
          if (updated[updated.length - 1]?.role === "ai") {
            updated[updated.length - 1].text = currentText;
          } else {
            updated.push({ role: "ai", text: currentText });
          }
          return [...updated];
        });

        if (i >= fullText.length) {
          clearInterval(interval);
          setLoading(false);
        }
      }, 15);

    } catch (err) {
      setLoading(false);
      console.log(err);
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "❌ Error getting response" },
      ]);
    }
  };

  // 📤 FILE UPLOAD (🔥 FIXED)
  const uploadFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    console.log("📂 File:", file);
    console.log("🔑 Token:", token);

    const formData = new FormData();
    formData.append("file", file);

    try {
      await axios.post(
        `${BASE_URL}/api/documents/upload`,
        formData,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );

      alert("✅ Uploaded successfully");
    } catch (err) {
      console.log("❌ Upload error:", err);
      alert("❌ Upload failed: " + (err.response?.data || err.message));
    }
  };

  // 🔐 LOGIN UI
  if (!token) {
    return (
      <div className="flex h-screen">

        <div className="w-1/2 bg-gradient-to-br from-blue-600 to-purple-600 text-white flex justify-center items-center">
          <h1 className="text-4xl font-bold">DocuMind AI</h1>
        </div>

        <div className="w-1/2 flex justify-center items-center bg-gray-100">
          <div className="bg-white p-8 rounded-xl shadow-xl w-96">

            <h2 className="text-2xl font-bold mb-4">
              {isLogin ? "Login" : "Register"}
            </h2>

            <form autoComplete="off">

              <input
                autoComplete="off"
                name="user_random"
                className="w-full border p-2 mb-3 rounded"
                placeholder="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />

              <input
                autoComplete="new-password"
                name="pass_random"
                type="password"
                className="w-full border p-2 mb-3 rounded"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />

            </form>

            <button
              onClick={handleAuth}
              className="w-full bg-blue-600 text-white p-2 rounded"
            >
              {isLogin ? "Login" : "Register"}
            </button>

            <p
              className="mt-3 text-blue-600 cursor-pointer"
              onClick={() => setIsLogin(!isLogin)}
            >
              {isLogin
                ? "Don't have account? Register"
                : "Already have account? Login"}
            </p>

          </div>
        </div>
      </div>
    );
  }

  // 💬 CHAT UI
  return (
    <div className="flex h-screen bg-[#0f172a]">

      {/* SIDEBAR */}
      <div className="w-64 bg-[#020617] text-white p-4">

        <h2 className="text-xl font-bold mb-4">🤖 DocuMind</h2>

        <button
          className="bg-blue-600 text-white p-2 rounded mb-3 w-full"
          onClick={() => setMessages([])}
        >
          + New Chat
        </button>

        <label className="cursor-pointer bg-gray-500 text-white p-2 rounded w-full text-center block">
          Upload File
          <input type="file" className="hidden" onChange={uploadFile} />
        </label>

        <button
          className="mt-4 w-full bg-red-600 text-white p-2 rounded"
          onClick={() => setToken("")}
        >
          Logout
        </button>
      </div>

      {/* CHAT */}
      <div className="flex flex-col flex-1">

        {/* MESSAGES */}
        <div className="flex-1 p-6 overflow-y-auto">
          {messages.map((msg, i) => (
            <div
              key={i}
              className={`mb-4 flex ${
                msg.role === "user" ? "justify-end" : "justify-start"
              }`}
            >
              <div className={`max-w-xl px-4 py-3 rounded-2xl ${
                msg.role === "user"
                  ? "bg-blue-600 text-white"
                  : "bg-gray-800 text-white"
              }`}>
                <pre className="whitespace-pre-wrap font-sans">
                  {msg.text}
                </pre>
              </div>
            </div>
          ))}

          {loading && (
            <div className="text-gray-400 animate-pulse">
              🤖 Thinking...
            </div>
          )}

          <div ref={chatEndRef} />
        </div>

        {/* INPUT */}
        <div className="p-4 border-t flex items-end">

          <textarea
            ref={textareaRef}
            className="flex-1 p-3 rounded-xl resize-none outline-none"
            style={{ minHeight: "40px", maxHeight: "150px" }}
            value={question}
            placeholder="Send a message..."
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
              }
            }}
          />

          <button
            onClick={sendMessage}
            className="ml-3 bg-blue-600 text-white px-5 py-2 rounded-xl"
          >
            Send
          </button>

        </div>
      </div>
    </div>
  );
}