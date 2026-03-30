import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
});

// 🔐 Attach token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default function App() {

  // ================= AUTH =================
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState(localStorage.getItem("username") || "");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(localStorage.getItem("token") || "");

  // ================= DATA =================
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState("");
  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);

  const chatEndRef = useRef(null);

  // ================= AUTO SCROLL =================
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // ================= FETCH DOCUMENTS =================
  const fetchDocuments = async () => {
    if (!username) return;

    try {
      const res = await api.get("/documents/history", {
        params: { username },
      });
      setDocuments(res.data || []);
    } catch (e) {
      console.log(e);
    }
  };

  useEffect(() => {
    if (token) {
      fetchDocuments();
    }
  }, [token]);

  // ================= FETCH CHAT (PER DOC) =================
  const fetchChatHistory = async (docId) => {
    if (!username || !docId) return;

    try {
      const res = await api.get("/chat/history", {
        params: {
          username,
          documentId: docId,
        },
      });

      const formatted = res.data.flatMap((item) => [
        { role: "user", text: item.question },
        { role: "ai", text: item.answer },
      ]);

      setMessages(formatted);

    } catch (e) {
      console.log(e);
    }
  };

  // 🔥 LOAD WHEN DOCUMENT CHANGES
  useEffect(() => {
    if (selectedDocId) {
      fetchChatHistory(selectedDocId);
    }
  }, [selectedDocId]);

  // ================= AUTH =================
  const handleAuth = async () => {
    try {
      const url = isLogin ? "/auth/login" : "/auth/register";
      const res = await api.post(url, { username, password });

      if (isLogin) {
        const t = res.data.token || res.data;
        setToken(t);
        localStorage.setItem("token", t);
        localStorage.setItem("username", username);
      } else {
        alert("Registered! Login now");
        setIsLogin(true);
      }
    } catch {
      alert("Auth failed");
    }
  };

  // ================= CHAT =================
  const sendMessage = async () => {
    if (!question.trim()) return;

    if (!selectedDocId) {
      alert("Select document first");
      return;
    }

    const q = question;
    setQuestion("");

    setMessages((prev) => [...prev, { role: "user", text: q }]);
    setLoading(true);

    try {
      const res = await api.post("/chat/ask", null, {
        params: {
          question: q,
          username,
          documentId: selectedDocId,
        },
      });

      setMessages((prev) => [
        ...prev,
        { role: "ai", text: res.data },
      ]);

    } catch {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "❌ Error" },
      ]);
    }

    setLoading(false);
  };

  // ================= ENTER KEY =================
  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  // ================= UPLOAD =================
  const uploadFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", username);

    try {
      setUploading(true);
      await api.post("/documents/upload", formData);
      fetchDocuments();
    } catch {
      alert("Upload failed");
    } finally {
      setUploading(false);
    }
  };

  // ================= DELETE DOC =================
  const deleteDoc = async (id) => {
    try {
      await api.delete(`/documents/delete/${id}`);
      fetchDocuments();
      if (selectedDocId === id) {
        setMessages([]);
        setSelectedDocId("");
      }
    } catch {
      alert("Delete failed");
    }
  };

  // ================= CLEAR CHAT =================
  const clearChat = () => {
    setMessages([]);
  };

  // ================= LOGOUT =================
  const logout = () => {
    localStorage.clear();
    setToken("");
    setMessages([]);
    setDocuments([]);
  };

  // ================= LOGIN UI =================
  if (!token) {
    return (
      <div className="flex h-screen">

        <div className="w-1/2 bg-blue-600 flex items-center justify-center text-white text-4xl font-bold">
          DocuMind AI
        </div>

        <div className="w-1/2 flex items-center justify-center bg-gray-100">
          <div className="bg-white p-8 rounded-xl shadow-lg w-80">

            <h2 className="text-2xl font-bold mb-6 text-center">
              {isLogin ? "Login" : "Register"}
            </h2>

            <input
              className="w-full p-2 border rounded mb-4"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />

            <input
              type="password"
              className="w-full p-2 border rounded mb-4"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            <button
              onClick={handleAuth}
              className="w-full bg-blue-600 text-white p-2 rounded"
            >
              {isLogin ? "Login" : "Register"}
            </button>

            <p
              className="mt-4 text-center text-blue-600 cursor-pointer"
              onClick={() => setIsLogin(!isLogin)}
            >
              Create new account
            </p>

          </div>
        </div>
      </div>
    );
  }

  // ================= MAIN UI =================
  return (
    <div className="flex h-screen bg-gray-900 text-white">

      {/* SIDEBAR */}
      <div className="w-64 bg-black p-4 flex flex-col">

        <button onClick={clearChat} className="bg-blue-600 p-2 rounded mb-3">
          New Chat
        </button>

        <input type="file" onChange={uploadFile} />
        {uploading && <p className="text-sm mt-2">Uploading...</p>}

        <select
          className="text-black mt-3 p-1 rounded"
          value={selectedDocId}
          onChange={(e) => setSelectedDocId(e.target.value)}
        >
          <option value="">Select Document</option>
          {documents.map((d) => (
            <option key={d.id} value={d.id}>
              {d.fileName}
            </option>
          ))}
        </select>

        <div className="mt-4 space-y-2 overflow-y-auto">
          {documents.map((d) => (
            <div key={d.id} className="flex justify-between text-sm">
              <span>{d.fileName}</span>
              <button onClick={() => deleteDoc(d.id)}>❌</button>
            </div>
          ))}
        </div>

        <button onClick={logout} className="mt-auto bg-red-600 p-2 rounded">
          Logout
        </button>

      </div>

      {/* CHAT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 p-4 overflow-y-auto space-y-3">

          {messages.map((m, i) => (
            <div
              key={i}
              className={`p-2 rounded max-w-xl ${
                m.role === "user"
                  ? "bg-blue-600 ml-auto"
                  : "bg-gray-700"
              }`}
            >
              {m.text}
            </div>
          ))}

          {loading && <p>Thinking...</p>}

          <div ref={chatEndRef}></div>
        </div>

        {/* INPUT */}
        <div className="p-3 flex gap-2">

          <textarea
            className="flex-1 p-2 text-black rounded resize-none"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask something... (Enter to send)"
            rows={2}
          />

          <button
            onClick={sendMessage}
            disabled={loading}
            className={`px-6 rounded ${
              loading ? "bg-gray-500" : "bg-blue-600"
            }`}
          >
            {loading ? "..." : "Send"}
          </button>

        </div>
      </div>
    </div>
  );
}