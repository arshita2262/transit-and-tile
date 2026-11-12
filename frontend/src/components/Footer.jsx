import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="tt-footer">
      <div className="container">
        <div className="row gy-4 align-items-center">
          <div className="col-md-6 text-center text-md-start">
            <div className="d-flex align-items-center justify-content-center justify-content-md-start gap-2 mb-2">
              <span className="fw-bold text-dark">TRANSIT & TILE</span>
              <span className="badge bg-light text-primary border">Hotel Portal</span>
            </div>
            <p className="mb-0 text-muted small">
              Handcrafted destination stays across India. Authoritative pricing, instant confirmed bookings, and bespoke destination postcards.
            </p>
          </div>
          <div className="col-md-6 text-center text-md-end">
            <div className="d-flex justify-content-center justify-content-md-end gap-3 mb-2 small">
              <Link to="/" className="text-decoration-none text-muted">Home</Link>
              <Link to="/my-bookings" className="text-decoration-none text-muted">My Bookings</Link>
            </div>
            <p className="mb-0 text-muted small">
              &copy; {new Date().getFullYear()} Transit & Tile. Demo project with sample data.
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}
