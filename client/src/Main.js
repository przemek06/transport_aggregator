import React, { useState, useEffect } from 'react';
import "./App.css";

function Main() {
    const [src, setSrc] = useState('');
    const [dest, setDest] = useState('');
    const [time, setTime] = useState('');
    const [results, setResults] = useState([]);
    const [searches, setSearches] = useState(0);
    const [error, setError] = useState('');
    const [isFetchingSeats, setIsFetchingSeats] = useState(false);

    function formatDate(date) {
        const pad = (n) => n.toString().padStart(2, '0');
        const day = pad(date.getDate());
        const month = pad(date.getMonth() + 1);
        const hours = pad(date.getHours());
        const minutes = pad(date.getMinutes());
        return `${day}-${month} ${hours}:${minutes}`;
    }

    const fetchAvailableSeats = async (offersToQuery) => {
        if (offersToQuery.length === 0 || isFetchingSeats) return;

        setIsFetchingSeats(true);
        setError('');

        try {
            const response = await fetch('http://localhost:8080/reservations/available-seats', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(offersToQuery),
                credentials: 'include',
            });

            if (!response.ok) {
                 const errorBody = await response.text();
                throw new Error(`Failed to fetch available seats: ${response.status} ${response.statusText}. ${errorBody}`);
            }

            const updatedOffers = await response.json();

            setResults(currentResults => {
                const updatedSeatsMap = new Map();
                updatedOffers.forEach(offer => {
                    const offerKey = `${offer.src}-${offer.dest}-${offer.startTime}-${offer.endTime}`;
                    updatedSeatsMap.set(offerKey, offer.availableSeats);
                });

                return currentResults.map(offer => {
                     const offerKey = `${offer.src}-${offer.dest}-${offer.startTime}-${offer.endTime}`;
                    if (updatedSeatsMap.has(offerKey)) {
                        return {
                            ...offer,
                            availableSeats: updatedSeatsMap.get(offerKey),
                        };
                    }
                    return offer;
                });
            });

        } catch (err) {
            console.error("Error fetching available seats:", err);
            setError(`Error fetching available seats: ${err.message}`);
        } finally {
            setIsFetchingSeats(false);
        }
    };

    useEffect(() => {
        const handler = setTimeout(() => {
             if (results.length > 0 && !isFetchingSeats) {
                fetchAvailableSeats(results);
             }
        }, 50);

        return () => clearTimeout(handler);
    }, [results.length]);

    const handleSearch = () => {
        setResults([]);
        setSearches((prevSearches) => prevSearches + 1);

        const eventSource = new EventSource(`http://localhost:8080/query/offers/src/${encodeURIComponent(src)}/dest/${encodeURIComponent(dest)}/time/${encodeURIComponent(time)}`, {
            withCredentials: true
        });

        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            setResults((prevResults) => {
                 const newOffers = data.map(offer => ({ ...offer, availableSeats: null }));
                 return [...prevResults, ...newOffers];
             });
            console.log('New offers received:', data);
        };

        eventSource.onerror = (error) => {
            console.error('SSE error:', error);
            setSearches((prevSearches) => prevSearches > 0 ? prevSearches - 1 : 0);
            eventSource.close();
        };

        return () => {
             eventSource.close();
             setSearches(0);
        };
    };

    const handleBook = async (offerToBook) => {
        setError('');

        if (offerToBook.availableSeats === null || offerToBook.availableSeats === undefined || offerToBook.availableSeats <= 0) {
             setError("Cannot book: No available seats or seat availability unknown.");
            return;
        }

        try {
            const response = await fetch('http://localhost:8080/reservations', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(offerToBook),
                credentials: 'include',
            });

            if (!response.ok) {
                 const errorBody = await response.text();
                 if (response.status === 402) {
                      throw new Error(`Booking failed: No seats left for this offer.`);
                 }
                throw new Error(`Failed to book offer: ${response.status} ${response.statusText}. ${errorBody}`);
            }

            setResults(currentResults =>
                currentResults.map(offer => {
                    if (offer.src === offerToBook.src &&
                        offer.dest === offerToBook.dest &&
                        offer.startTime === offerToBook.startTime &&
                        offer.endTime === offerToBook.endTime &&
                         offer.cost === offerToBook.cost &&
                         offer.type === offerToBook.type
                    ) {
                        const newAvailableSeats = Math.max(0, (offer.availableSeats || 0) - 1);
                        return {
                            ...offer,
                            availableSeats: newAvailableSeats,
                        };
                    }
                    return offer;
                })
            );

        } catch (err) {
            console.error("Booking error:", err);
            setError(`Booking failed: ${err.message}`);
        }
    };

    return (
        <div>
            {error && <p style={{ color: 'red', fontWeight: 'bold' }}>{error}</p>}

            <h2>Search Offers</h2>
            <div>
                <label>Source:</label>
                <input type="text" value={src} onChange={(e) => setSrc(e.target.value)} />
            </div>
            <div>
                <label>Destination:</label>
                <input type="text" value={dest} onChange={(e) => setDest(e.target.value)} />
            </div>
            <div>
                <label>Time:</label>
                <input type="datetime-local" value={time} onChange={(e) => setTime(e.target.value)} />
            </div>
            <button onClick={handleSearch} disabled={searches > 0}>Search</button>

            {(searches > 0) && <p>Fetching offers...</p>}
            {(isFetchingSeats) && <p>Updating seat availability...</p>}

            {results.length > 0 && (
                <div>
                    <h3>Results:</h3>
                    <table>
                        <thead>
                            <tr>
                                <th>Source</th>
                                <th>Destination</th>
                                <th>Start Time</th>
                                <th>End Time</th>
                                <th>Cost</th>
                                <th>Type</th>
                                <th>Vehicles</th>
                                <th>Available seats</th>
                                <th>Book</th>
                            </tr>
                        </thead>
                        <tbody>
                            {results.map((offer, offerIndex) => (
                                <tr key={offerIndex}>
                                    <td>{offer.src}</td>
                                    <td>{offer.dest}</td>
                                    <td>{formatDate(new Date(offer.startTime))}</td>
                                    <td>{formatDate(new Date(offer.endTime))}</td>
                                    <td>{offer.cost}</td>
                                    <td>{offer.type}</td>
                                    <td>
                                        <table>
                                            <thead>
                                                <tr>
                                                    <th>Vehicle ID</th>
                                                    <th>Start</th>
                                                    <th>End</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {(Array.isArray(offer.vehicles) ? offer.vehicles : []).map((vehicle, vIndex) => (
                                                    <tr key={vIndex}>
                                                        <td>{vehicle.id}</td>
                                                        <td>{formatDate(new Date(vehicle.start))}</td>
                                                        <td>{formatDate(new Date(vehicle.end))}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </td>
                                    <td>
                                        {offer.availableSeats === null || offer.availableSeats === undefined ? '?' : offer.availableSeats}
                                    </td>
                                    <td>
                                        <button
                                            onClick={() => handleBook(offer)}
                                            disabled={offer.availableSeats === null || offer.availableSeats === undefined || offer.availableSeats <= 0}
                                        >
                                            Book
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}

export default Main;