import React,{useState} from "react";
import api from "../api/api";

export default function UploadPanel(){

    const [file,setFile] = useState(null);

    const upload = async () => {

        if(!file){
            alert("Select file");
            return;
        }

        const formData = new FormData();
        formData.append("file",file);

        try{

            await api.post("/documents/upload",formData,{
                headers:{
                    "Content-Type":"multipart/form-data"
                }
            });

            alert("Upload successful");

        }catch(err){

            alert("Upload failed");

        }

    };

    return(
        <div>

            <h3>Upload Document</h3>

            <input type="file"
                   onChange={(e)=>setFile(e.target.files[0])}
            />

            <button onClick={upload}>
                Upload
            </button>

        </div>
    );
}