import { useState, useEffect, useRef } from "react";
import axios from "axios";

const BASE_URL =
  process.env.REACT_APP_API_URL ||
  "https://documind-backend-30m4.onrender.com/api";

const api = axios.create({
  baseURL: BASE_URL,
});

// ✅ TOKEN
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default function App() {

  // 🔐 AUTH
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState(localStorage.getItem("username") || "");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState(localStorage.getItem("token") || "");

  // 💬 CHAT
  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);

  // 📂 DOCS
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState(null);
  const [uploading, setUploading] = useState(false);

  const textareaRef = useRef(null);

  // =========================
  // 📂 FETCH DOCUMENTS
  // =========================
  const fetchDocuments = async () => {
    try {
      const res = await api.get("/documents/history", {
        params: { username },
      });
      setDocuments(res.data || []);
    } catch (err) {
      console.log("❌ Document error", err);
    }
  };

  useEffect(() => {
    if (token) fetchDocuments();
  }, [token]);

  // =========================
  // 🔐 AUTH
  // =========================
  const handleAuth = async () => {
    const url = isLogin ? "/auth/login" : "/auth/register";

    try {
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
    } catch (e) {
      alert("❌ " + e.message);
    }
  };

  // =========================
  // 💬 SEND MESSAGE (FIXED)
  // =========================
  const sendMessage = async () => {

    if (!question.trim()) return;

    if (!selectedDocId) {
      alert("⚠ Select document first");
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

    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: "ai", text: "❌ Error" },
      ]);
    }

    setLoading(false);
  };

  // =========================
  // 📤 UPLOAD
  // =========================
  const uploadFile = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", username);

    try {
      setUploading(true);

      await api.post("/documents/upload", formData);

      alert("✅ Uploaded");
      fetchDocuments();

    } catch {
      alert("❌ Upload failed");
    } finally {
      setUploading(false);
    }
  };

  // =========================
  // ❌ DELETE DOC
  // =========================
  const deleteDoc = async (id) => {
    await api.delete(`/documents/delete/${id}`);
    fetchDocuments();
  };

  // =========================
  // 🧹 CLEAR CHAT
  // =========================
  const clearChat = () => {
    setMessages([]);
  };

  // =========================
  // 🚪 LOGOUT
  // =========================
  const logout = () => {
    localStorage.clear();
    setToken("");
  };

  // =========================
  // 🔐 LOGIN UI
  // =========================
  if (!token) {
    return (
      <div className="flex h-screen">
        <div className="w-1/2 bg-blue-600 flex items-center justify-center text-white text-3xl">
          DocuMind AI
        </div>

        <div className="w-1/2 flex items-center justify-center">
          <div>
            <input placeholder="Username" onChange={(e) => setUsername(e.target.value)} />
            <input type="password" placeholder="Password" onChange={(e) => setPassword(e.target.value)} />

            <button onClick={handleAuth}>
              {isLogin ? "Login" : "Register"}
            </button>

            <p onClick={() => setIsLogin(!isLogin)}>Switch</p>
          </div>
        </div>
      </div>
    );
  }

  // =========================
  // 💬 MAIN UI
  // =========================
  return (
    <div className="flex h-screen bg-gray-900 text-white">

      {/* LEFT */}
      <div className="w-64 bg-black p-4 flex flex-col">

        <button onClick={clearChat} className="bg-blue-600 p-2 mb-3">
          New Chat
        </button>

        <input type="file" onChange={uploadFile} />
        {uploading && <p>Uploading...</p>}

        {/* SELECT DOC */}
        <select
          onChange={(e) => setSelectedDocId(e.target.value)}
          className="text-black mt-2"
        >
          <option>Select Document</option>
          {documents.map((d) => (
            <option key={d.id} value={d.id}>
              {d.fileName}
            </option>
          ))}
        </select>

        <div className="mt-4">
          {documents.map((d) => (
            <div key={d.id} className="flex justify-between">
              <span>{d.fileName}</span>
              <button onClick={() => deleteDoc(d.id)}>❌</button>
            </div>
          ))}
        </div>

        <button onClick={logout} className="mt-auto bg-red-600 p-2">
          Logout
        </button>

      </div>

      {/* RIGHT */}
      <div className="flex-1 flex flex-col">

        <div className="flex-1 p-4 overflow-y-auto">
          {messages.map((m, i) => (
            <div key={i} className={m.role === "user" ? "text-right" : ""}>
              {m.text}
            </div>
          ))}
          {loading && <p>Thinking...</p>}
        </div>

        <div className="p-3 flex">
          <textarea
            className="flex-1 text-black"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
          />
          <button onClick={sendMessage} className="bg-blue-600 px-4">
            Send
          </button>
        </div>

      </div>
    </div>
  );
}