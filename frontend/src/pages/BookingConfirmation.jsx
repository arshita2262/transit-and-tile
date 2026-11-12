import { useState } from 'react';
import { useLocation, useParams, Link } from 'react-router-dom';
import { getDestinationDetails } from '../utils/destinationData';
import { formatCurrency, formatDateDisplay } from '../utils/dateUtils';
import { lookupBooking } from '../services/api';

export default function BookingConfirmation() {
  const { reference } = useParams();
  const location = useLocation();

  // Booking details passed via router state from BookingDetails page
  const [booking, setBooking] = useState(location.state?.booking || null);
  const [loading, setLoading] = useState(false);
  const [lookupEmail, setLookupEmail] = useState('');
  const [lookupError, setLookupError] = useState('');
  const [isFlipped, setIsFlipped] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const handleVerifyLookup = async (e) => {
    e.preventDefault();
    if (!lookupEmail.trim()) return;
    try {
      setLoading(true);
      setLookupError('');
      const data = await lookupBooking(reference, lookupEmail.trim());
      setBooking(data);
    } catch {
      setLookupError('Could not verify booking reference and email combination.');
    } finally {
      setLoading(false);
    }
  };

  const destination = getDestinationDetails(booking?.hotelLocation || '');

  // Genuine Postcard Download implementation using HTML5 Canvas
  const handleDownloadPostcard = () => {
    setDownloading(true);
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 900;
      canvas.height = 560;
      const ctx = canvas.getContext('2d');

      // 1. Vintage Cream Paper Background
      ctx.fillStyle = '#faf7f2';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // 2. Vintage Postal Border
      ctx.strokeStyle = '#d6c9b8';
      ctx.lineWidth = 4;
      ctx.strokeRect(16, 16, canvas.width - 32, canvas.height - 32);

      ctx.strokeStyle = '#e2e8f0';
      ctx.lineWidth = 1;
      ctx.strokeRect(24, 24, canvas.width - 48, canvas.height - 48);

      // 3. Header: Brand & Destination
      ctx.fillStyle = '#0369a1';
      ctx.font = 'bold 26px sans-serif';
      ctx.fillText('TRANSIT & TILE • DESTINATION POSTCARD', 48, 64);

      ctx.fillStyle = '#ea580c';
      ctx.font = 'italic 18px serif';
      ctx.fillText(destination.greeting, 48, 96);

      // 4. Center dashed vertical divider
      ctx.strokeStyle = '#cbd5e1';
      ctx.lineWidth = 2;
      ctx.setLineDash([8, 8]);
      ctx.beginPath();
      ctx.moveTo(canvas.width / 2, 120);
      ctx.lineTo(canvas.width / 2, canvas.height - 40);
      ctx.stroke();
      ctx.setLineDash([]); // reset line dash

      // 5. Left Side: Booking Message & Travel Notes
      ctx.fillStyle = '#0f172a';
      ctx.font = 'bold 18px sans-serif';
      ctx.fillText(`Dear ${booking.customerName},`, 48, 145);

      ctx.font = '14px sans-serif';
      ctx.fillStyle = '#334155';
      ctx.fillText(`Your stay at ${booking.hotelName} is officially CONFIRMED!`, 48, 175);
      ctx.fillText(`Room: ${booking.roomName} (1 Room Reserved)`, 48, 198);
      ctx.fillText(`Dates: ${formatDateDisplay(booking.checkIn)} to ${formatDateDisplay(booking.checkOut)} (${booking.nights} Nights)`, 48, 221);
      ctx.fillText(`Total Paid/Charged: ${formatCurrency(booking.totalAmount)}`, 48, 244);

      // Curated Tip Box
      ctx.fillStyle = '#f1f5f9';
      ctx.fillRect(48, 275, 360, 140);
      ctx.strokeStyle = '#cbd5e1';
      ctx.strokeRect(48, 275, 360, 140);

      ctx.fillStyle = '#0369a1';
      ctx.font = 'bold 14px sans-serif';
      ctx.fillText(`✨ LOCAL HIGHLIGHT: ${destination.curatedAttraction.name}`, 60, 305);

      ctx.font = '12px sans-serif';
      ctx.fillStyle = '#475569';
      ctx.fillText(destination.curatedAttraction.description.slice(0, 52), 60, 330);
      ctx.fillText(destination.curatedAttraction.description.slice(52, 105), 60, 350);

      ctx.fillStyle = '#b45309';
      ctx.font = 'italic 12px sans-serif';
      ctx.fillText(`💡 Tip: ${destination.localTip.slice(0, 50)}...`, 60, 385);

      // 6. Right Side: Postal Stamp & Tracking Reference
      // Stamp box
      const stampX = canvas.width - 160;
      const stampY = 135;
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(stampX, stampY, 110, 130);
      ctx.strokeStyle = '#94a3b8';
      ctx.lineWidth = 2;
      ctx.strokeRect(stampX, stampY, 110, 130);

      ctx.fillStyle = '#0284c7';
      ctx.font = 'bold 12px sans-serif';
      ctx.fillText('POSTAGE', stampX + 25, stampY + 28);
      ctx.fillText(destination.stampLabel, stampX + 10, stampY + 52);
      ctx.font = '32px sans-serif';
      ctx.fillText(destination.illustrativeWeather.icon, stampX + 38, stampY + 95);

      // Circular Postmark
      ctx.strokeStyle = '#64748b';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(stampX - 25, stampY + 65, 38, 0, Math.PI * 2);
      ctx.stroke();

      ctx.font = 'bold 9px sans-serif';
      ctx.fillStyle = '#475569';
      ctx.fillText('CONFIRMED', stampX - 56, stampY + 60);
      ctx.fillText(booking.checkIn, stampX - 56, stampY + 75);

      // Booking Reference barcode/tracking representation
      ctx.fillStyle = '#0f172a';
      ctx.font = 'bold 15px monospace';
      ctx.fillText('BOOKING TRACKING REFERENCE:', canvas.width / 2 + 30, 320);

      ctx.font = 'bold 22px monospace';
      ctx.fillStyle = '#0369a1';
      ctx.fillText(booking.bookingReference, canvas.width / 2 + 30, 350);

      ctx.font = '12px sans-serif';
      ctx.fillStyle = '#64748b';
      ctx.fillText(`Customer: ${booking.customerName}`, canvas.width / 2 + 30, 385);
      ctx.fillText(`Destination: ${booking.hotelLocation}`, canvas.width / 2 + 30, 405);
      ctx.fillText(`Sample Weather: ${destination.illustrativeWeather.temp} (${destination.illustrativeWeather.condition})*`, canvas.width / 2 + 30, 425);

      // Footer note
      ctx.font = 'italic 11px sans-serif';
      ctx.fillStyle = '#94a3b8';
      ctx.fillText('* Illustrative sample weather for trip planning purposes. Not a live forecast.', 48, canvas.height - 24);

      // Trigger actual browser download
      const link = document.createElement('a');
      link.download = `Transit_and_Tile_Postcard_${booking.bookingReference}.png`;
      link.href = canvas.toDataURL('image/png');
      link.click();
    } catch (err) {
      console.error('Download error:', err);
    } finally {
      setDownloading(false);
    }
  };

  // If user refreshed directly on this URL without state, prompt for email to verify and unlock
  if (!booking) {
    return (
      <div className="container py-5 text-center">
        <div className="card p-4 mx-auto border shadow-sm" style={{ maxWidth: '480px' }}>
          <div className="fs-1 mb-2">📮</div>
          <h4 className="fw-bold mb-1">Retrieve Your Postcard</h4>
          <p className="text-muted small mb-3">
            Reference: <strong>{reference}</strong>
          </p>
          <p className="text-muted small mb-4">
            For security, please enter the email address used during booking to display your keepsake postcard.
          </p>

          <form onSubmit={handleVerifyLookup}>
            <div className="mb-3 text-start">
              <label htmlFor="verifyEmail" className="form-label small fw-bold text-muted">Booking Email</label>
              <input
                id="verifyEmail"
                type="email"
                className="form-control"
                placeholder="name@example.com"
                value={lookupEmail}
                onChange={(e) => setLookupEmail(e.target.value)}
                required
              />
            </div>
            {lookupError && (
              <div className="alert alert-danger py-2 small mb-3" role="alert">
                {lookupError}
              </div>
            )}
            <button type="submit" className="btn btn-tt-primary w-100" disabled={loading}>
              {loading ? 'Verifying...' : 'Unlock Postcard'}
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="booking-confirmation-page py-5" style={{ background: '#f8fafc', minHeight: '85vh' }}>
      <div className="container">
        {/* Success Banner */}
        <div className="text-center mb-4">
          <div className="d-inline-flex align-items-center gap-2 bg-success-subtle text-success border border-success-subtle px-3 py-1 rounded-pill mb-2">
            <span>✓</span>
            <span className="fw-bold small">Booking Confirmed & Inventory Locked</span>
          </div>
          <h1 className="h2 fw-bold text-dark mb-1">Destination Postcard Confirmation</h1>
          <p className="text-muted small">
            Keep this postcard safe. Your official booking reference is{' '}
            <strong className="text-primary font-monospace">{booking.bookingReference}</strong>
          </p>
        </div>

        {/* Vintage Postcard Container with 3D Flip */}
        <div className="tt-postcard-wrapper mb-4">
          <div className="tt-postcard">
            {/* Front View */}
            {!isFlipped ? (
              <div>
                {/* Postcard Header */}
                <div className="d-flex justify-content-between align-items-start border-bottom pb-3 mb-3">
                  <div>
                    <span className="badge bg-primary text-white text-uppercase tracking-wider mb-1" style={{ fontSize: '0.7rem' }}>
                      Official Postcard Keepsake
                    </span>
                    <h3 className="h4 fw-bold text-dark mb-0">{destination.greeting}</h3>
                    <small className="text-muted">{booking.hotelLocation}</small>
                  </div>
                  <div className="d-flex align-items-center gap-2">
                    {/* Postmark stamp */}
                    <div className="tt-postmark d-none d-sm-flex">
                      <span>CONFIRMED</span>
                      <span style={{ fontSize: '0.6rem' }}>{booking.checkIn}</span>
                      <span>T&T</span>
                    </div>
                    {/* Postage Stamp */}
                    <div className="tt-stamp-badge">
                      <span className="text-muted" style={{ fontSize: '0.65rem' }}>{destination.stampLabel}</span>
                      <span className="fs-3 my-1">{destination.illustrativeWeather.icon}</span>
                      <span className="fw-bold text-primary" style={{ fontSize: '0.65rem' }}>INDIA POST</span>
                    </div>
                  </div>
                </div>

                {/* Two-Column Postcard Body */}
                <div className="row g-4">
                  {/* Left Column: Personalized Note & Curated Highlight */}
                  <div className="col-md-6 tt-postcard-divider">
                    <h5 className="fw-bold mb-2">Dear {booking.customerName},</h5>
                    <p className="text-muted small mb-3 leading-relaxed">
                      Your stay at <strong>{booking.hotelName}</strong> ({booking.roomName}) is locked in. We are thrilled to welcome you for {booking.nights} memorable {booking.nights === 1 ? 'night' : 'nights'}!
                    </p>

                    {/* Curated Local Destination Tip */}
                    <div className="p-3 rounded bg-white border mb-3 shadow-sm">
                      <span className="fw-bold text-primary small d-block mb-1">
                        ✨ Curated Attraction: {destination.curatedAttraction.name}
                      </span>
                      <p className="text-muted small mb-2" style={{ fontSize: '0.8rem' }}>
                        {destination.curatedAttraction.description}
                      </p>
                      <div className="p-2 rounded bg-light small" style={{ fontSize: '0.78rem' }}>
                        💡 <strong>Local Tip:</strong> {destination.localTip}
                      </div>
                    </div>

                    {/* Predefined Sample Weather (Explicitly Labelled) */}
                    <div className="p-2 px-3 rounded bg-light border small">
                      <div className="d-flex justify-content-between align-items-center">
                        <span className="fw-semibold text-muted">
                          {destination.illustrativeWeather.icon} Illustrative Weather:
                        </span>
                        <span className="fw-bold text-dark">
                          {destination.illustrativeWeather.temp} &bull; {destination.illustrativeWeather.condition}
                        </span>
                      </div>
                      <small className="text-muted d-block mt-1" style={{ fontSize: '0.7rem' }}>
                        * Predefined illustrative sample weather for planning. Not a live forecast.
                      </small>
                    </div>
                  </div>

                  {/* Right Column: Address and Reservation Metadata */}
                  <div className="col-md-6 d-flex flex-column justify-content-between">
                    <div>
                      <div className="mb-3 p-3 bg-white rounded border">
                        <span className="text-muted small d-block text-uppercase fw-bold" style={{ fontSize: '0.7rem' }}>
                          Postal Tracking Code
                        </span>
                        <span className="h4 fw-bold font-monospace text-primary d-block mb-0">
                          {booking.bookingReference}
                        </span>
                      </div>

                      <div className="mb-2">
                        <span className="text-muted small d-block">Addressed To:</span>
                        <span className="fw-bold text-dark">{booking.customerName}</span>
                        <span className="text-muted small d-block">{booking.email}</span>
                        <span className="text-muted small d-block">{booking.phone}</span>
                      </div>

                      <div className="pt-2 border-top">
                        <div className="d-flex justify-content-between text-muted small mb-1">
                          <span>Check-in:</span>
                          <span className="fw-bold text-dark">{formatDateDisplay(booking.checkIn)}</span>
                        </div>
                        <div className="d-flex justify-content-between text-muted small mb-1">
                          <span>Check-out:</span>
                          <span className="fw-bold text-dark">{formatDateDisplay(booking.checkOut)}</span>
                        </div>
                        <div className="d-flex justify-content-between text-muted small mb-1">
                          <span>Night Rate:</span>
                          <span>{formatCurrency(booking.pricePerNight)}</span>
                        </div>
                        <div className="d-flex justify-content-between fw-bold pt-1 border-top text-dark">
                          <span>Authoritative Total:</span>
                          <span className="text-primary">{formatCurrency(booking.totalAmount)}</span>
                        </div>
                      </div>
                    </div>

                    <div className="mt-3 text-center text-md-end">
                      <span className="badge bg-success text-white">
                        ● Status: {booking.status}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              /* Back View: Itinerary & Voucher Overview */
              <div>
                <div className="d-flex justify-content-between align-items-center border-bottom pb-3 mb-3">
                  <h4 className="fw-bold mb-0">Postcard Back: Stay Voucher Details</h4>
                  <span className="badge bg-secondary font-monospace">{booking.bookingReference}</span>
                </div>
                <div className="p-3 bg-white rounded border mb-3">
                  <h6 className="fw-bold text-primary mb-2">Check-in Instructions & Policy</h6>
                  <p className="text-muted small mb-2">
                    Present your booking reference <strong>{booking.bookingReference}</strong> and government ID upon arrival at <strong>{booking.hotelName}</strong>.
                  </p>
                  <p className="text-muted small mb-0">
                    Standard check-in is 2:00 PM; check-out is 11:00 AM. Free cancellation is permitted up to 48 hours prior to check-in via the "My Bookings" portal.
                  </p>
                </div>
                <div className="p-3 bg-light rounded border text-muted small">
                  <strong>Need to cancel or review?</strong> Go to <em>My Bookings</em> with your reference code <code>{booking.bookingReference}</code> and email <code>{booking.email}</code>.
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Postcard Action Toolbar */}
        <div className="d-flex flex-wrap justify-content-center align-items-center gap-3">
          <button
            type="button"
            className="btn btn-outline-secondary"
            onClick={() => setIsFlipped(!isFlipped)}
          >
            🔄 {isFlipped ? 'View Postcard Front' : 'Flip to Voucher Back'}
          </button>

          <button
            type="button"
            className="btn btn-tt-primary"
            onClick={handleDownloadPostcard}
            disabled={downloading}
          >
            📥 {downloading ? 'Preparing PNG...' : 'Download Postcard (.png)'}
          </button>

          <Link
            to="/my-bookings"
            state={{ reference: booking.bookingReference, email: booking.email }}
            className="btn btn-tt-outline"
          >
            Manage in My Bookings →
          </Link>

          <Link to="/" className="btn btn-link text-decoration-none text-muted">
            Book Another Stay
          </Link>
        </div>
      </div>
    </div>
  );
}
