import React, { useState, useEffect } from 'react';
import './Updates.css'; // You can create this for basic styling

function Updates() {
    const [updates, setUpdates] = useState([]);
    const [error, setError] = useState('');
    const MAX_UPDATES = 10;

    // Helper function to format dates
    const formatDate = (dateString) => {
        if (!dateString) return '';
        const date = new Date(dateString);
        const pad = (n) => n.toString().padStart(2, '0');
        const day = pad(date.getDate());
        const month = pad(date.getMonth() + 1);
        const hours = pad(date.getHours());
        const minutes = pad(date.getMinutes());
        return `${day}-${month} ${hours}:${minutes}`;
    };

    // Function to fetch initial last 10 updates
    const fetchLastUpdates = async () => {
        setError('');
        try {
            const response = await fetch('http://localhost:8080/import/updates/last', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',
            });

            if (!response.ok) {
                const errorBody = await response.text();
                throw new Error(`Failed to fetch last updates: ${response.status} ${response.statusText}. ${errorBody}`);
            }

            const data = await response.json();
            setUpdates(data.slice(0, MAX_UPDATES)); // Ensure only the last MAX_UPDATES are shown
        } catch (err) {
            console.error("Error fetching last updates:", err);
            setError(`Error fetching last updates: ${err.message}`);
        }
    };

    // Effect for fetching initial updates on component mount
    useEffect(() => {
        fetchLastUpdates();
    }, []);

    // Effect for SSE updates
    useEffect(() => {
        const eventSource = new EventSource('http://localhost:8080/import/updates', {
            withCredentials: true
        });

        eventSource.onmessage = (event) => {
            const newUpdate = JSON.parse(event.data);
            setUpdates((prevUpdates) => {
                const updatedList = [newUpdate, ...prevUpdates];
                return updatedList.slice(0, MAX_UPDATES); // Keep only the last MAX_UPDATES
            });
            console.log('New update received:', newUpdate);
        };

        eventSource.onerror = (error) => {
            console.error('SSE error:', error);
            setError(`SSE connection error: ${error.message || 'An unknown error occurred.'}`);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, []);

    const renderUpdateDetails = (update) => {
        switch (update.operation) {
            case 'CREATE':
                const created = update.inserted;
                return (
                    <>
                        <p><strong>Operation:</strong> CREATE</p>
                        <p><strong>Source:</strong> {created.src}</p>
                        <p><strong>Destination:</strong> {created.dest}</p>
                        <p><strong>Start Time:</strong> {formatDate(created.startTime)}</p>
                        <p><strong>End Time:</strong> {formatDate(created.endTime)}</p>
                        <p><strong>Cost:</strong> {created.cost}</p>
                        <p><strong>Type:</strong> {created.type}</p>
                        <p><strong>Max Seats:</strong> {created.maxSeats}</p>
                        {created.vehicles && created.vehicles.length > 0 && (
                            <div>
                                <p><strong>Vehicles:</strong></p>
                                <ul>
                                    {created.vehicles.map((vehicle, vIndex) => (
                                        <li key={vIndex}>
                                            ID: {vehicle.id}, Start: {formatDate(vehicle.start)}, End: {formatDate(vehicle.end)}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </>
                );
            case 'UPDATE':
                const updated = update.updated;
                return (
                    <>
                        <p><strong>Operation:</strong> UPDATE</p>
                        <p><strong>Offer ID:</strong> {updated.id}</p>
                        {updated.price !== undefined && <p><strong>New Price:</strong> {updated.price}</p>}
                        {updated.delay !== undefined && <p><strong>New Delay:</strong> {updated.delay}</p>}
                        {updated.maxSeats !== undefined && <p><strong>New Max Seats:</strong> {updated.maxSeats}</p>}
                    </>
                );
            case 'DELETE':
                const deletedId = update.deleted;
                return (
                    <>
                        <p><strong>Operation:</strong> DELETE</p>
                        <p><strong>Offer ID:</strong> {deletedId}</p>
                    </>
                );
            default:
                return <p>Unknown operation.</p>;
        }
    };

    return (
        <div className="updates-container">
            <h2>Recent Updates</h2>
            {error && <p style={{ color: 'red', fontWeight: 'bold' }}>{error}</p>}
            {updates.length === 0 && !error && <p>No updates to display yet.</p>}
            <div className="updates-list">
                {updates.map((update, index) => (
                    <div key={index} className="update-item">
                        {renderUpdateDetails(update)}
                    </div>
                ))}
            </div>
        </div>
    );
}

export default Updates;