import React, { useEffect, useState } from "react";
import axios from "axios";

function App() {

  // =========================
  // 🔥 STATES
  // =========================
  const [username, setUsername] = useState("laxman");
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState(null);
  const [question, setQuestion] = useState("");
  const [chat, setChat] = useState([]);
  const [file, setFile] = useState(null);

  const BASE_URL = "https://documind-backend-30m4.onrender.com";

  // =========================
  // 🔥 LOAD DATA
  // =========================
  useEffect(() => {
    fetchDocuments();
    fetchChatHistory();
  }, []);

  // =========================
  // 📄 FETCH DOCUMENTS
  // =========================
  const fetchDocuments = async () => {
    try {
      const res = await axios.get(
        `${BASE_URL}/api/documents/history?username=${username}`
      );
      setDocuments(res.data);
    } catch (err) {
      console.log("❌ Error fetching documents", err);
    }
  };

  // =========================
  // 💬 FETCH CHAT HISTORY
  // =========================
  const fetchChatHistory = async () => {
    try {
      const res = await axios.get(
        `${BASE_URL}/api/chat/history?username=${username}`
      );
      setChat(res.data);
    } catch (err) {
      console.log("❌ Chat history error", err);
    }
  };

  // =========================
  // 📤 UPLOAD FILE
  // =========================
  const uploadFile = async () => {
    if (!file) {
      alert("Select file first");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("username", username);

    try {
      await axios.post(
        `${BASE_URL}/api/documents/upload`,
        formData
      );

      alert("✅ Uploaded");
      fetchDocuments();
    } catch (err) {
      console.log(err);
      alert("❌ Upload failed");
    }
  };

  // =========================
  // ❓ ASK QUESTION
  // =========================
  const askQuestion = async () => {
    if (!question.trim()) return;

    if (!selectedDocId) {
      alert("⚠ Select document first");
      return;
    }

    try {
      setChat((prev) => [...prev, "Q: " + question]);

      const res = await axios.post(
        `${BASE_URL}/api/chat/ask`,
        null,
        {
          params: {
            question,
            username,
            documentId: selectedDocId,
          },
        }
      );

      setChat((prev) => [...prev, "A: " + res.data]);

      setQuestion("");
    } catch (err) {
      console.log(err);
      alert("❌ Error asking question");
    }
  };

  // =========================
  // 🧹 CLEAR CHAT
  // =========================
  const clearChat = async () => {
    await axios.delete(
      `${BASE_URL}/api/chat/clear?username=${username}`
    );
    setChat([]);
  };

  // =========================
  // 🎨 UI
  // =========================
  return (
    <div style={{ display: "flex", height: "100vh" }}>

      {/* LEFT PANEL */}
      <div style={{
        width: "25%",
        background: "#000",
        color: "#fff",
        padding: "15px"
      }}>

        <h3>📄 Upload</h3>

        <input
          type="file"
          onChange={(e) => setFile(e.target.files[0])}
        />

        <button onClick={uploadFile} style={{ marginTop: "10px" }}>
          Upload
        </button>

        <hr />

        <h3>📂 Documents</h3>

        <select
          onChange={(e) => setSelectedDocId(e.target.value)}
          style={{ width: "100%", marginBottom: "10px" }}
        >
          <option value="">Select Document</option>
          {documents.map((doc) => (
            <option key={doc.id} value={doc.id}>
              {doc.fileName}
            </option>
          ))}
        </select>

        <ul>
          {documents.map((doc) => (
            <li key={doc.id}>{doc.fileName}</li>
          ))}
        </ul>

      </div>

      {/* RIGHT PANEL */}
      <div style={{
        width: "75%",
        background: "#0b1a2b",
        color: "#fff",
        padding: "20px"
      }}>

        <h2>💬 DocuMind AI</h2>

        {/* CHAT */}
        <div style={{
          height: "70%",
          overflowY: "scroll",
          border: "1px solid gray",
          padding: "10px",
          marginBottom: "10px"
        }}>
          {chat.map((msg, i) => (
            <p key={i}>{msg}</p>
          ))}
        </div>

        {/* INPUT */}
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask question..."
          style={{ width: "80%", padding: "10px" }}
        />

        <button onClick={askQuestion} style={{ padding: "10px" }}>
          Send
        </button>

        <br /><br />

        <button onClick={clearChat}>
          Clear Chat
        </button>

      </div>
    </div>
  );
}

export default App;