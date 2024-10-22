import React, {useState} from 'react';
import './App.css';
import {Box, Button, TextField} from "@mui/material";

function App() {
    const [selectedFile, setSelectedFile] = useState(null);

    const handleFileChange = (event: any) => {
        setSelectedFile(event.target.files[0]);
    };

    const handleUpload = async () => {
        if (!selectedFile) {
            return;
        }

        const migrationDetails = new FormData();
        migrationDetails.append('migrationRequest', getBody());
        migrationDetails.append('migrationScripts', selectedFile);

        try {
            const response = await fetch('http://localhost:8080/migrate', {
                method: 'POST',
                // headers: {'Content-Type': 'multipart/form-data'},
                body: migrationDetails
            });

            if (!response.ok) {
                throw new Error('File upload failed');
            }

            // Handle successful upload, e.g., display a success message
            console.log('File uploaded successfully');
        } catch (error) {
            // Handle upload error, e.g., display an error message
            console.error(error);
        }
    };

    function getBody() {
        return JSON.stringify({
            projectId: 'my-project-id',
            datasetId: 'my-dataset-id',
            credentials: 'my-credentials',
            replacements: {}
        });
    }

    return (
        //        <div>
        //             <input type="file" onChange={handleFileChange}/>
        //             <Button onClick={handleUpload} variant="contained">Upload</Button>
        //         </div>

        <Box
            component="form"
            sx={{'& .MuiTextField-root': {m: 1, width: '25ch'}}}
            noValidate
            autoComplete="off"
        >
            <div>
                <TextField
                    required
                    id="outlined-required"
                    label="Required"
                    defaultValue="Hello World"
                />
                <TextField
                    disabled
                    id="outlined-disabled"
                    label="Disabled"
                    defaultValue="Hello World"
                />
                <TextField
                    id="outlined-password-input"
                    label="Password"
                    type="password"
                    autoComplete="current-password"
                />
                <TextField
                    id="outlined-read-only-input"
                    label="Read Only"
                    defaultValue="Hello World"
                    slotProps={{
                        input: {
                            readOnly: true,
                        },
                    }}
                />
                <TextField
                    id="outlined-number"
                    label="Number"
                    type="number"
                    slotProps={{
                        inputLabel: {
                            shrink: true,
                        },
                    }}
                />
                <TextField id="outlined-search" label="Search field" type="search"/>
                <TextField
                    id="outlined-helperText"
                    label="Helper text"
                    defaultValue="Default Value"
                    helperText="Some important text"
                />
            </div>
            <div>
                <TextField
                    required
                    id="filled-required"
                    label="Required"
                    defaultValue="Hello World"
                    variant="filled"
                />
                <TextField
                    disabled
                    id="filled-disabled"
                    label="Disabled"
                    defaultValue="Hello World"
                    variant="filled"
                />
                <TextField
                    id="filled-password-input"
                    label="Password"
                    type="password"
                    autoComplete="current-password"
                    variant="filled"
                />
                <TextField
                    id="filled-read-only-input"
                    label="Read Only"
                    defaultValue="Hello World"
                    variant="filled"
                    slotProps={{
                        input: {
                            readOnly: true,
                        },
                    }}
                />
                <TextField
                    id="filled-number"
                    label="Number"
                    type="number"
                    variant="filled"
                    slotProps={{
                        inputLabel: {
                            shrink: true,
                        },
                    }}
                />
                <TextField
                    id="filled-search"
                    label="Search field"
                    type="search"
                    variant="filled"
                />
                <TextField
                    id="filled-helperText"
                    label="Helper text"
                    defaultValue="Default Value"
                    helperText="Some important text"
                    variant="filled"
                />
            </div>
            <div>
                <TextField
                    required
                    id="standard-required"
                    label="Required"
                    defaultValue="Hello World"
                    variant="standard"
                />
                <TextField
                    disabled
                    id="standard-disabled"
                    label="Disabled"
                    defaultValue="Hello World"
                    variant="standard"
                />
                <TextField
                    id="standard-password-input"
                    label="Password"
                    type="password"
                    autoComplete="current-password"
                    variant="standard"
                />
                <TextField
                    id="standard-read-only-input"
                    label="Read Only"
                    defaultValue="Hello World"
                    variant="standard"
                    slotProps={{
                        input: {
                            readOnly: true,
                        },
                    }}
                />
                <TextField
                    id="standard-number"
                    label="Number"
                    type="number"
                    variant="standard"
                    slotProps={{
                        inputLabel: {
                            shrink: true,
                        },
                    }}
                />
                <TextField
                    id="standard-search"
                    label="Search field"
                    type="search"
                    variant="standard"
                />
                <TextField
                    id="standard-helperText"
                    label="Helper text"
                    defaultValue="Default Value"
                    helperText="Some important text"
                    variant="standard"
                />
                <input type="file" onChange={handleFileChange}/>
                <Button onClick={handleUpload} variant="contained">Upload</Button>
            </div>
        </Box>
    );
}

export default App;
