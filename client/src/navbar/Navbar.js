import React from 'react';
import { Link } from 'react-router-dom';
import './Navbar.css';

function Navbar() {
  return (
    <nav className="navbar">
      <ul>
        <li>
          <Link to="/main">Main</Link>
        </li>
        <li>
          <Link to="/reservations">Reservation</Link>
        </li>
        <li>
          <Link to="/import">Import</Link>
        </li>
      </ul>
    </nav>
  );
}

export default Navbar;