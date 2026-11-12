import { Link, useLocation } from 'react-router-dom';

export default function Navbar() {
  const location = useLocation();

  return (
    <nav className="tt-navbar sticky-top">
      <div className="container d-flex justify-content-between align-items-center">
        <Link to="/" className="tt-brand">
          <div className="tt-brand-badge">T&T</div>
          <span>TRANSIT & TILE</span>
        </Link>
        <div className="d-flex align-items-center gap-2">
          <Link
            to="/"
            className={`tt-nav-link ${location.pathname === '/' || location.pathname === '/search' ? 'active' : ''}`}
          >
            Explore Stays
          </Link>
          <Link
            to="/my-bookings"
            className={`tt-nav-link ${location.pathname === '/my-bookings' ? 'active' : ''}`}
          >
            My Bookings
          </Link>
        </div>
      </div>
    </nav>
  );
}
