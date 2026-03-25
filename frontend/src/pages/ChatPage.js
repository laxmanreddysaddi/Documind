import { useEffect, useState } from "react";
import {
  askQuestion,
  uploadFile,
  getHistory,
  getDocuments,
  clearHistory
} from "../api";

export default function ChatPage({ user, logout }) {
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [documents, setDocuments] = useState([]);

  const token = localStorage.getItem("token");

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    const history = await getHistory(user);
    const docs = await getDocuments(token);
    setMessages(history);
    setDocuments(docs);
  };

  const sendMessage = async () => {
    const answer = await askQuestion(question, user);
    setMessages([...messages, { question, answer }]);
    setQuestion("");
  };

  const handleUpload = async (e) => {
    await uploadFile(e.target.files[0], token);
    alert("Uploaded!");
    loadData();
  };

  const handleClear = async () => {
    await clearHistory(user);
    setMessages([]);
  };

  return (
    <div className="flex h-screen">

      {/* Sidebar */}
      <div className="w-1/4 bg-gray-100 p-4">
        <h2 className="font-bold mb-2">📂 Documents</h2>
        {documents.map(doc => (
          <div key={doc.id}>
            {doc.fileName}
          </div>
        ))}

        <input type="file" onChange={handleUpload}
          className="mt-4" />

        <button onClick={handleClear}
          className="mt-4 bg-red-500 text-white p-2 w-full">
          Clear Chat
        </button>

        <button onClick={logout}
          className="mt-2 bg-gray-500 text-white p-2 w-full">
          Logout
        </button>
      </div>

      {/* Chat */}
      <div className="flex-1 p-4 flex flex-col">
        <div className="flex-1 overflow-y-auto">
          {messages.map((msg, i) => (
            <div key={i} className="mb-4">
              <b>You:</b> {msg.question}
              <br />
              <b>AI:</b> {msg.answer}
            </div>
          ))}
        </div>

        <div className="flex">
          <input
            className="flex-1 border p-2"
            value={question}
            onChange={e => setQuestion(e.target.value)}
          />
          <button
            onClick={sendMessage}
            className="bg-blue-500 text-white p-2">
            Send
          </button>
        </div>
      </div>

    </div>
  );
}