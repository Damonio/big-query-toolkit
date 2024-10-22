import React, {useState} from 'react';
import './App.css';
import {Box, Button, TextField} from "@mui/material";
import MigrationService from "./MigrationService";

function App() {
    const [credentials, setCredentials] = useState("");
    const [environmentFileName, setEnvironmentFileName] = useState("");
    const [migrationScripts, setMigrationScripts] = useState(null);

    const handleFileChange = (event: any) => {
        setMigrationScripts(event.target.files[0]);
    };

    const handleCredentialsChange = (event: any) => {
        setCredentials(event.target.value);
    };

    const handleEnvironmentFileNameChange = (event: any) => {
        setEnvironmentFileName(event.target.value);
    };

    const handleUpload = async () => {
        if (!migrationScripts) {
            return;
        }

        const migrationDetails = new FormData();
        migrationDetails.append('environmentFileName', environmentFileName);
        migrationDetails.append('credentials', credentials);
        migrationDetails.append('migrationScripts', migrationScripts);

        MigrationService.migrate(migrationDetails).then(data => {
            if (data) console.log('File uploaded successfully');
            else console.error('File upload failed');
        });
    };

    return (
        <Box
            component="form"
            sx={{'& .MuiTextField-root': {m: 1, width: '25ch'}}}
            noValidate
            autoComplete="off"
        >
            <div>
                <TextField
                    required
                    id="environment-file-name"
                    label="Environment file name"
                    defaultValue="application-dev.yml"
                    variant="filled"
                    onChange={handleEnvironmentFileNameChange}
                />
                <TextField
                    id="compressed-encoded-credentials"
                    label="Compressed Encoded Credentials"
                    type="password"
                    autoComplete="current-password"
                    variant="filled"
                    onChange={handleCredentialsChange}
                />
                <input type="file" onChange={handleFileChange}/>
                <Button onClick={handleUpload} variant="contained">Upload</Button>
            </div>
        </Box>
    );
}

export default App;
