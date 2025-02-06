import os
import pandas as pd
from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse, HTMLResponse
from pydantic import BaseModel

app = FastAPI()

# Define the CSV file path
csv_file = 'data.csv'

# Check if the CSV file exists; if not, create one with headers
if not os.path.exists(csv_file):
    df = pd.DataFrame(columns=["Unique ID", "Name", "Longitude", "Latitude", "Floor"])
    df.to_csv(csv_file, index=False)

# Define the data model for incoming data
class LocationData(BaseModel):
    name: str
    uniqueID: str
    floor: int
    latitude: float  # Use float for latitude (double in Python)
    longitude: float  # Use float for longitude (double in Python)

@app.post("/submit-data")
async def submit_data(data: LocationData):
    try:
        # Extract the details from the incoming data
        unique_id = data.uniqueID
        name = data.name
        longitude = data.longitude
        latitude = data.latitude
        floor = data.floor
        
        # Append the data to the CSV file
        new_data = pd.DataFrame([[unique_id, name, longitude, latitude, floor]], 
                                 columns=["Unique ID", "Name", "Longitude", "Latitude", "Floor"])
        new_data.to_csv(csv_file, mode='a', header=False, index=False)
        
        # Return a success response
        return {"message": "Data received successfully"}

    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/get-csv")
async def get_csv():
    # Check if the file exists before serving
    if os.path.exists(csv_file):
        return FileResponse(csv_file, media_type='text/csv', filename='data.csv')
    else:
        raise HTTPException(status_code=404, detail="CSV file not found")

@app.get("/", response_class=HTMLResponse)
async def index():
    # Return a simple HTML page with a map for real-time tracking
    return """
   <!DOCTYPE html>
<html>
<head>
    <title>Real-Time Location Tracking</title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://unpkg.com/leaflet/dist/leaflet.css" />
    <style>
        body { font-family: Arial, sans-serif; }
        h1 { color: #333; }
        #map { height: 600px; width: 100%; }
    </style>
</head>
<body>
    <h1>Real-Time Location Tracking</h1>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>
    <script>
        // Initialize the map with a specific zoom level
        var map = L.map('map').setView([20.5937, 78.9629], 15); // Set to a constant zoom level

        // Add OpenStreetMap tiles
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
        }).addTo(map);

        var marker;

        async function fetchLatestLocation() {
            const response = await fetch('/latest-location?unique_id=YOUR_UNIQUE_ID');  // Add the unique ID query parameter
            if (response.ok) {
                const data = await response.json();
                if (marker) {
                    map.removeLayer(marker);  // Remove the old marker
                }
                marker = L.marker([data.latitude, data.longitude]).addTo(map);
                // Set the view without changing the zoom level
                map.setView([data.latitude, data.longitude]);  // Center the map on the new location
            } else {
                console.error("Error fetching location:", response.statusText);
            }
        }

        // Fetch the latest location every 1 second
        setInterval(fetchLatestLocation, 500);
    </script>
</body>
</html>
    """

@app.get("/latest-location")
async def latest_location():
    if os.path.exists(csv_file):
        df = pd.read_csv(csv_file)
        if not df.empty:
            latest_entry = df.iloc[-1].to_dict()
            return {
                "uniqueID": latest_entry["Unique ID"],
                "name": latest_entry["Name"],
                "latitude": latest_entry["Latitude"],
                "longitude": latest_entry["Longitude"],
                "floor": latest_entry["Floor"]
            }
        else:
            raise HTTPException(status_code=404, detail="No data available")
    else:
        raise HTTPException(status_code=404, detail="CSV file not found")

if __name__ == "__main__":
    import uvicorn
    
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
