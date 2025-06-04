import React from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate, useLocation } from 'react-router-dom';
import Login from './Login';
import Main from './Main';
import Navbar from './navbar/Navbar';
import Reservations from './Reservations';
import Import from './Import';
import Updates from './updates/Updates';

function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

function AppContent() {
  const location = useLocation();
  const showNavbar = location.pathname !== '/login'; 

  return (
    <>
      {showNavbar && <Navbar />}
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/main" element={<Main />} />
        <Route path="/reservations" element={<Reservations />} />
        <Route path="/import" element={<Import />} />
        <Route path="/updates" element={<Updates />} />
        <Route path="/" element={<Navigate replace to="/login" />} />
      </Routes>
    </>
  );
}

export default App;