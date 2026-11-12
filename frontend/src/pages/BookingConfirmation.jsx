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

  // Genuine Postcard Download — premium travel-journal design via HTML5 Canvas
  const handleDownloadPostcard = () => {
    setDownloading(true);
    try {
      const W = 940;
      const H = 580;
      const HERO_H = 120;
      const PAD = 48;
      const MID = W / 2;

      const canvas = document.createElement('canvas');
      canvas.width = W;
      canvas.height = H;
      const ctx = canvas.getContext('2d');

      // ── 1. Linen-cream paper background ─────────────────────────────────
      ctx.fillStyle = '#fdf8f0';
      ctx.fillRect(0, 0, W, H);

      // Subtle dot grain
      for (let x = 0; x < W; x += 20) {
        for (let y = HERO_H; y < H; y += 20) {
          ctx.fillStyle = 'rgba(180,163,140,0.18)';
          ctx.beginPath();
          ctx.arc(x, y, 0.7, 0, Math.PI * 2);
          ctx.fill();
        }
      }

      // Outer border (double-line postal frame)
      ctx.strokeStyle = '#c8b89a';
      ctx.lineWidth = 3;
      ctx.strokeRect(10, 10, W - 20, H - 20);
      ctx.strokeStyle = '#e2d5c3';
      ctx.lineWidth = 1;
      ctx.strokeRect(16, 16, W - 32, H - 32);

      // ── 2. Destination hero header ───────────────────────────────────────
      const accent = destination.colorAccent || '#0284c7';

      // Hero gradient band
      const heroGrad = ctx.createLinearGradient(0, 0, W, HERO_H);
      heroGrad.addColorStop(0, accent);
      heroGrad.addColorStop(0.6, accent + 'cc');
      heroGrad.addColorStop(1, '#00000055');
      ctx.fillStyle = heroGrad;
      ctx.fillRect(0, 0, W, HERO_H);

      // Brand line
      ctx.fillStyle = 'rgba(255,255,255,0.72)';
      ctx.font = '600 11px sans-serif';
      ctx.fillText('TRANSIT & TILE  ·  DESTINATION POSTCARD  ·  OFFICIAL KEEPSAKE', PAD, 22);

      // Destination name — large hero text
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 46px serif';
      ctx.fillText(destination.name.toUpperCase(), PAD, 82);

      // Tagline
      ctx.fillStyle = 'rgba(255,255,255,0.84)';
      ctx.font = 'italic 14px serif';
      ctx.fillText(destination.tagline, PAD, 106);

      // Weather chip (top-right of hero)
      ctx.fillStyle = 'rgba(255,255,255,0.18)';
      ctx.beginPath();
      ctx.roundRect(W - 180, 24, 158, 46, 8);
      ctx.fill();
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 20px sans-serif';
      ctx.fillText(destination.illustrativeWeather.icon, W - 170, 58);
      ctx.font = 'bold 13px sans-serif';
      ctx.fillText(destination.illustrativeWeather.temp, W - 145, 48);
      ctx.font = '11px sans-serif';
      ctx.fillStyle = 'rgba(255,255,255,0.85)';
      ctx.fillText(destination.illustrativeWeather.condition, W - 145, 64);

      // ── 3. Hero stamp + postmark ─────────────────────────────────────────
      // Stamp box (rotated)
      const sX = W - 100;
      const sY = HERO_H + 20;
      ctx.save();
      ctx.translate(sX + 40, sY + 50);
      ctx.rotate(0.055);
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(-40, -50, 80, 100);
      ctx.strokeStyle = '#94a3b8';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([4, 3]);
      ctx.strokeRect(-40, -50, 80, 100);
      ctx.setLineDash([]);
      ctx.fillStyle = accent;
      ctx.font = 'bold 8px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(destination.stampLabel, 0, -28);
      ctx.font = '28px sans-serif';
      ctx.fillText(destination.illustrativeWeather.icon, 0, 8);
      ctx.fillStyle = '#64748b';
      ctx.font = 'bold 7px sans-serif';
      ctx.fillText('INDIA POST', 0, 36);
      ctx.textAlign = 'left';
      ctx.restore();

      // Postmark circle
      const pmX = W - 160;
      const pmY = HERO_H + 68;
      ctx.strokeStyle = '#64748b';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(pmX, pmY, 32, 0, Math.PI * 2);
      ctx.stroke();
      ctx.fillStyle = '#475569';
      ctx.font = 'bold 8px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('CONFIRMED', pmX, pmY - 8);
      ctx.fillText(booking.checkIn || '', pmX, pmY + 4);
      ctx.fillText('T & T', pmX, pmY + 15);
      ctx.textAlign = 'left';

      // ── 4. Left column: personalized note + highlight ────────────────────
      const bodyY = HERO_H + 28;
      const leftW = MID - PAD - 24;

      // Greeting in serif italic
      ctx.fillStyle = '#3d2f1a';
      ctx.font = 'italic 15px Georgia, serif';
      ctx.fillText(destination.greeting, PAD, bodyY + 20);

      // Salutation
      ctx.fillStyle = '#0f172a';
      ctx.font = 'bold 17px sans-serif';
      ctx.fillText(`Dear ${booking.customerName},`, PAD, bodyY + 50);

      ctx.font = '13px sans-serif';
      ctx.fillStyle = '#334155';
      const line1 = `Your stay at ${booking.hotelName}`;
      const line2 = `(${booking.roomName}) is officially CONFIRMED!`;
      const line3 = `${formatDateDisplay(booking.checkIn)}  →  ${formatDateDisplay(booking.checkOut)}  ·  ${booking.nights} ${booking.nights === 1 ? 'night' : 'nights'}`;
      ctx.fillText(line1, PAD, bodyY + 73);
      ctx.fillText(line2, PAD, bodyY + 90);
      ctx.fillStyle = '#475569';
      ctx.fillText(line3, PAD, bodyY + 112);

      // Highlight box
      const hBoxY = bodyY + 132;
      const hBoxH = 148;
      ctx.fillStyle = '#ffffff';
      ctx.beginPath();
      ctx.roundRect(PAD, hBoxY, leftW, hBoxH, 8);
      ctx.fill();
      ctx.strokeStyle = '#e2d9cc';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.roundRect(PAD, hBoxY, leftW, hBoxH, 8);
      ctx.stroke();

      // Accent left border on highlight box
      ctx.fillStyle = accent;
      ctx.fillRect(PAD, hBoxY, 3, hBoxH);

      ctx.fillStyle = accent;
      ctx.font = 'bold 11px sans-serif';
      ctx.fillText('✨  CURATED ATTRACTION', PAD + 14, hBoxY + 22);
      ctx.fillStyle = '#0f172a';
      ctx.font = 'bold 13px sans-serif';
      ctx.fillText(destination.curatedAttraction.name, PAD + 14, hBoxY + 42);

      // Wrap attraction description
      ctx.font = '11.5px sans-serif';
      ctx.fillStyle = '#475569';
      const desc = destination.curatedAttraction.description;
      const maxChars = 54;
      const dLine1 = desc.length > maxChars ? desc.slice(0, desc.lastIndexOf(' ', maxChars)) : desc;
      const dLine2 = desc.slice(dLine1.length).trim();
      ctx.fillText(dLine1, PAD + 14, hBoxY + 62);
      if (dLine2) ctx.fillText(dLine2.slice(0, 65), PAD + 14, hBoxY + 78);

      // Tip
      ctx.fillStyle = '#92400e';
      ctx.font = 'italic 11px sans-serif';
      const tip = `💡 ${destination.localTip}`;
      ctx.fillText(tip.slice(0, 60), PAD + 14, hBoxY + 102);
      if (tip.length > 60) ctx.fillText(tip.slice(60, 118), PAD + 14, hBoxY + 118);

      // Weather note at base of left column
      ctx.fillStyle = '#94a3b8';
      ctx.font = 'italic 9.5px sans-serif';
      ctx.fillText('* Illustrative sample weather — not a live forecast.', PAD, H - 22);

      // ── 5. Center dashed divider ─────────────────────────────────────────
      ctx.strokeStyle = '#c8b89a';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([7, 7]);
      ctx.beginPath();
      ctx.moveTo(MID, HERO_H + 16);
      ctx.lineTo(MID, H - 20);
      ctx.stroke();
      ctx.setLineDash([]);

      // ── 6. Right column: booking reference block + details ───────────────
      const rX = MID + 24;
      const rW = W - rX - PAD;

      // "Addressed To" label
      ctx.fillStyle = '#94a3b8';
      ctx.font = 'bold 9px sans-serif';
      ctx.fillText('ADDRESSED TO:', rX, bodyY + 20);

      ctx.fillStyle = '#0f172a';
      ctx.font = 'bold 15px sans-serif';
      ctx.fillText(booking.customerName, rX, bodyY + 40);

      ctx.fillStyle = '#64748b';
      ctx.font = '11px sans-serif';
      ctx.fillText(booking.email || '', rX, bodyY + 58);
      ctx.fillText(booking.phone || '', rX, bodyY + 74);
      ctx.fillText(booking.hotelLocation || '', rX, bodyY + 90);

      // Booking reference block
      const refBoxY = bodyY + 110;
      ctx.fillStyle = '#f8f4ee';
      ctx.beginPath();
      ctx.roundRect(rX, refBoxY, rW, 70, 8);
      ctx.fill();
      ctx.strokeStyle = '#ddd0be';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.roundRect(rX, refBoxY, rW, 70, 8);
      ctx.stroke();

      ctx.fillStyle = '#94a3b8';
      ctx.font = 'bold 8.5px sans-serif';
      ctx.fillText('BOOKING REFERENCE', rX + 14, refBoxY + 20);
      ctx.fillStyle = accent;
      ctx.font = 'bold 26px monospace';
      ctx.fillText(booking.bookingReference || '', rX + 14, refBoxY + 54);

      // Date & pricing rows
      const priceY = refBoxY + 90;
      const drawRow = (label, value, y, bold = false) => {
        ctx.font = bold ? 'bold 13px sans-serif' : '12px sans-serif';
        ctx.fillStyle = bold ? '#0f172a' : '#64748b';
        ctx.fillText(label, rX, y);
        ctx.textAlign = 'right';
        ctx.fillStyle = bold ? accent : '#334155';
        ctx.fillText(value, rX + rW, y);
        ctx.textAlign = 'left';
      };

      ctx.strokeStyle = '#e2d9cc';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(rX, priceY - 10);
      ctx.lineTo(rX + rW, priceY - 10);
      ctx.stroke();

      drawRow('Check-in:', formatDateDisplay(booking.checkIn), priceY);
      drawRow('Check-out:', formatDateDisplay(booking.checkOut), priceY + 18);
      drawRow('Night Rate:', formatCurrency(booking.pricePerNight), priceY + 36);

      ctx.strokeStyle = '#c8b89a';
      ctx.beginPath();
      ctx.moveTo(rX, priceY + 46);
      ctx.lineTo(rX + rW, priceY + 46);
      ctx.stroke();

      drawRow('Total Charged:', formatCurrency(booking.totalAmount), priceY + 62, true);

      // Status badge
      ctx.fillStyle = '#dcfce7';
      ctx.beginPath();
      ctx.roundRect(rX, priceY + 78, 90, 24, 6);
      ctx.fill();
      ctx.fillStyle = '#166534';
      ctx.font = 'bold 10px sans-serif';
      ctx.fillText(`● ${booking.status || 'CONFIRMED'}`, rX + 10, priceY + 94);

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

        {/* Vintage Postcard Container */}
        <div className="tt-postcard-wrapper mb-4">
          <div className="tt-postcard">
            {/* Front View */}
            {!isFlipped ? (
              <div>
                {/* ── Destination Hero Banner ── */}
                <div
                  className="tt-postcard-hero"
                  style={{ background: `linear-gradient(135deg, ${destination.colorAccent} 0%, ${destination.colorAccent}99 60%, #00000055 100%)` }}
                >
                  <div className="tt-postcard-hero-text">
                    <div style={{ color: 'rgba(255,255,255,0.75)', fontSize: '0.68rem', fontWeight: 700, letterSpacing: '1.5px', textTransform: 'uppercase', marginBottom: '0.3rem' }}>
                      Official Postcard Keepsake · Transit &amp; Tile
                    </div>
                    <h2 className="tt-postcard-destination-name">{destination.name.toUpperCase()}</h2>
                    <p className="tt-postcard-tagline">{destination.tagline}</p>
                  </div>

                  <div className="tt-postcard-hero-right">
                    {/* Weather chip */}
                    <div style={{ background: 'rgba(255,255,255,0.18)', backdropFilter: 'blur(6px)', borderRadius: '10px', padding: '0.45rem 0.75rem', textAlign: 'center', color: '#fff', minWidth: '90px' }}>
                      <div style={{ fontSize: '1.4rem', lineHeight: 1 }}>{destination.illustrativeWeather.icon}</div>
                      <div style={{ fontSize: '0.78rem', fontWeight: 700 }}>{destination.illustrativeWeather.temp}</div>
                      <div style={{ fontSize: '0.62rem', opacity: 0.85 }}>{destination.illustrativeWeather.condition}</div>
                    </div>

                    {/* Postmark */}
                    <div className="tt-postmark d-none d-sm-flex">
                      <span>CONFIRMED</span>
                      <span style={{ fontSize: '0.55rem' }}>{booking.checkIn}</span>
                      <span>T&amp;T</span>
                    </div>

                    {/* Stamp */}
                    <div className="tt-stamp-badge">
                      <span className="tt-stamp-label">{destination.stampLabel}</span>
                      <span style={{ fontSize: '1.6rem', margin: '0.3rem 0' }}>{destination.illustrativeWeather.icon}</span>
                      <span className="tt-stamp-brand">INDIA POST</span>
                    </div>
                  </div>
                </div>

                {/* ── Postcard Body ── */}
                <div className="tt-postcard-body">
                  <div className="row g-4">
                    {/* Left Column */}
                    <div className="col-md-6 tt-postcard-divider pe-md-4">
                      <p className="tt-postcard-greeting">{destination.greeting}</p>
                      <h5 className="fw-bold mb-1" style={{ color: '#1e293b' }}>Dear {booking.customerName},</h5>
                      <p className="mb-3" style={{ fontSize: '0.88rem', color: '#475569', lineHeight: 1.55 }}>
                        Your stay at <strong style={{ color: '#0f172a' }}>{booking.hotelName}</strong> ({booking.roomName}) is locked in.
                        We&apos;re delighted to welcome you for{' '}
                        <strong>{booking.nights} {booking.nights === 1 ? 'night' : 'nights'}</strong>!
                      </p>

                      {/* Curated Attraction */}
                      <div className="rounded mb-3" style={{ background: '#fff', border: '1px solid #e2d9cc', padding: '0.9rem 1rem', borderLeft: `3px solid ${destination.colorAccent}` }}>
                        <span className="d-block fw-bold mb-1" style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.6px', color: destination.colorAccent }}>
                          ✨ Curated Attraction
                        </span>
                        <span className="d-block fw-semibold mb-1" style={{ fontSize: '0.88rem', color: '#1e293b' }}>
                          {destination.curatedAttraction.name}
                        </span>
                        <p className="mb-2" style={{ fontSize: '0.8rem', color: '#64748b', lineHeight: 1.5 }}>
                          {destination.curatedAttraction.description}
                        </p>
                        <div style={{ background: '#fdf8f0', borderRadius: '6px', padding: '0.45rem 0.6rem', fontSize: '0.78rem', color: '#92400e' }}>
                          💡 <strong>Local Tip:</strong> {destination.localTip}
                        </div>
                      </div>

                      {/* Illustrative Weather */}
                      <div className="tt-postcard-weather">
                        <div className="d-flex justify-content-between align-items-center">
                          <span style={{ color: '#64748b', fontWeight: 600, fontSize: '0.78rem' }}>
                            {destination.illustrativeWeather.icon} Illustrative Weather
                          </span>
                          <span style={{ fontWeight: 700, fontSize: '0.82rem', color: '#1e293b' }}>
                            {destination.illustrativeWeather.temp} &bull; {destination.illustrativeWeather.condition}
                          </span>
                        </div>
                        <small className="d-block mt-1" style={{ fontSize: '0.68rem', color: '#94a3b8', fontStyle: 'italic' }}>
                          * Predefined illustrative sample — not a live forecast.
                        </small>
                      </div>
                    </div>

                    {/* Right Column */}
                    <div className="col-md-6 d-flex flex-column justify-content-between">
                      {/* Booking Reference */}
                      <div className="tt-postcard-ref-block mb-3">
                        <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '0.3rem' }}>
                          Booking Reference
                        </div>
                        <div className="font-monospace fw-bold" style={{ fontSize: '1.55rem', color: destination.colorAccent, letterSpacing: '1px' }}>
                          {booking.bookingReference}
                        </div>
                      </div>

                      {/* Addressed To */}
                      <div className="tt-postcard-address mb-3" style={{ borderLeftColor: destination.colorAccent }}>
                        <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '0.3rem' }}>
                          Addressed To
                        </div>
                        <div className="fw-bold" style={{ fontSize: '1rem', color: '#1e293b' }}>{booking.customerName}</div>
                        <div style={{ fontSize: '0.82rem', color: '#64748b' }}>{booking.email}</div>
                        <div style={{ fontSize: '0.82rem', color: '#64748b' }}>{booking.phone}</div>
                        <div style={{ fontSize: '0.82rem', color: '#64748b' }}>{booking.hotelLocation}</div>
                      </div>

                      {/* Stay Details */}
                      <div style={{ background: '#fff', border: '1px solid #e2d9cc', borderRadius: '10px', padding: '0.9rem 1rem' }}>
                        <div className="tt-postcard-price-row">
                          <span>Check-in</span>
                          <span className="fw-semibold" style={{ color: '#1e293b' }}>{formatDateDisplay(booking.checkIn)}</span>
                        </div>
                        <div className="tt-postcard-price-row">
                          <span>Check-out</span>
                          <span className="fw-semibold" style={{ color: '#1e293b' }}>{formatDateDisplay(booking.checkOut)}</span>
                        </div>
                        <div className="tt-postcard-price-row">
                          <span>Night Rate</span>
                          <span>{formatCurrency(booking.pricePerNight)}</span>
                        </div>
                        <div className="tt-postcard-price-row total">
                          <span>Total Charged</span>
                          <span style={{ color: destination.colorAccent }}>{formatCurrency(booking.totalAmount)}</span>
                        </div>
                      </div>

                      <div className="mt-3 d-flex justify-content-end">
                        <span className="badge bg-success text-white px-3 py-2" style={{ fontSize: '0.78rem' }}>
                          ● {booking.status}
                        </span>
                      </div>
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
