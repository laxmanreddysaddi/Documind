import React from "react";
import ChatPanel from "../components/ChatPanel";
import UploadPanel from "../components/UploadPanel";
import ChatHistory from "../components/ChatHistory";
import DocumentHistory from "../components/DocumentHistory";

export default function Dashboard(){

    return(

        <div style={{display:"flex",height:"100vh"}}>

            {/* LEFT SIDEBAR */}
            <div style={{
                width:"20%",
                borderRight:"1px solid gray",
                padding:"10px",
                overflowY:"auto"
            }}>
                <ChatHistory/>
            </div>

            {/* CHAT AREA */}
            <div style={{
                width:"50%",
                padding:"20px"
            }}>
                <ChatPanel/>
            </div>

            {/* RIGHT PANEL */}
            <div style={{
                width:"30%",
                borderLeft:"1px solid gray",
                padding:"10px",
                overflowY:"auto"
            }}>
                <UploadPanel/>
                <DocumentHistory/>
            </div>

        </div>

    );

}