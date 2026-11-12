import { useState, useEffect } from 'react';
import { useLocation, Link } from 'react-router-dom';
import { lookupBooking, cancelBooking } from '../services/api';
import { formatCurrency, formatDateDisplay } from '../utils/dateUtils';

export default function MyBookings() {
  const location = useLocation();

  // If navigated from confirmation or with prefilled state
  const initialRef = location.state?.reference || '';
  const initialEmail = location.state?.email || '';

  const [bookingReference, setBookingReference] = useState(initialRef);
  const [email, setEmail] = useState(initialEmail);
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState('');
  const [cancelSuccessMsg, setCancelSuccessMsg] = useState('');
  const [showCancelModal, setShowCancelModal] = useState(false);

  // Auto-lookup if both initial reference and email are supplied
  useEffect(() => {
    if (!initialRef || !initialEmail) return;

    let isMounted = true;
    async function autoFetch() {
      try {
        setLoading(true);
        const data = await lookupBooking(initialRef.trim(), initialEmail.trim());
        if (isMounted) setBooking(data);
      } catch (err) {
        if (isMounted) {
          setError(err.status === 404
            ? 'No reservation found matching this booking reference and email combination.'
            : (err.message || 'Unable to retrieve booking details.'));
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    }
    autoFetch();
    return () => { isMounted = false; };
  }, [initialRef, initialEmail]);

  const handleLookupSubmit = async (e) => {
    if (e) e.preventDefault();
    setError('');
    setCancelSuccessMsg('');
    setBooking(null);

    if (!bookingReference.trim() || !email.trim()) {
      setError('Both booking reference and email address are required.');
      return;
    }

    try {
      setLoading(true);
      const data = await lookupBooking(bookingReference.trim(), email.trim());
      setBooking(data);
    } catch (err) {
      if (err.status === 404) {
        setError('No reservation found matching this booking reference and email combination.');
      } else {
        setError(err.message || 'Unable to retrieve booking details. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async () => {
    if (!booking) return;
    try {
      setCancelling(true);
      setError('');
      const updated = await cancelBooking(booking.bookingReference, booking.email);
      setBooking(updated);
      setShowCancelModal(false);
      setCancelSuccessMsg('Your booking has been successfully cancelled. The room inventory has been released.');
    } catch (err) {
      setError(err.message || 'Unable to cancel booking. It may have already been cancelled.');
      setShowCancelModal(false);
    } finally {
      setCancelling(false);
    }
  };

  return (
    <div className="my-bookings-page py-5">
      <div className="container" style={{ maxWidth: '780px' }}>
        <div className="text-center mb-4">
          <h1 className="h2 fw-bold text-dark mb-1">Manage Your Reservation</h1>
          <p className="text-muted small">
            Retrieve your booking details or request a cancellation using your booking reference and email.
          </p>
        </div>

        {/* Lookup Form */}
        <div className="card p-4 border bg-white rounded shadow-sm mb-4">
          <form onSubmit={handleLookupSubmit}>
            <div className="row g-3">
              <div className="col-md-6">
                <label htmlFor="lookupRef" className="form-label fw-semibold small text-muted">
                  Booking Reference Code
                </label>
                <input
                  id="lookupRef"
                  type="text"
                  className="form-control font-monospace text-uppercase"
                  placeholder="e.g. TT-GOA-4891"
                  value={bookingReference}
                  onChange={(e) => setBookingReference(e.target.value)}
                  maxLength={30}
                  required
                />
              </div>

              <div className="col-md-6">
                <label htmlFor="lookupEmailInput" className="form-label fw-semibold small text-muted">
                  Booking Email Address
                </label>
                <input
                  id="lookupEmailInput"
                  type="email"
                  className="form-control"
                  placeholder="name@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  maxLength={254}
                  required
                />
              </div>

              <div className="col-12 mt-3">
                <button
                  type="submit"
                  className="btn btn-tt-primary w-100 py-2"
                  disabled={loading}
                >
                  {loading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Verifying & Retrieving...
                    </>
                  ) : (
                    'Retrieve Reservation'
                  )}
                </button>
              </div>
            </div>
          </form>
        </div>

        {/* Error Notification */}
        {error && (
          <div className="alert alert-danger py-3 px-4 mb-4 shadow-sm" role="alert">
            <div className="d-flex align-items-center gap-2">
              <span>⚠️</span>
              <div>{error}</div>
            </div>
          </div>
        )}

        {/* Cancellation Success Notice */}
        {cancelSuccessMsg && (
          <div className="alert alert-success py-3 px-4 mb-4 shadow-sm" role="alert">
            <div className="d-flex align-items-center gap-2">
              <span>✓</span>
              <div>{cancelSuccessMsg}</div>
            </div>
          </div>
        )}

        {/* Booking Card Display */}
        {booking && (
          <div className="card border bg-white rounded shadow-sm overflow-hidden mb-4">
            {/* Card Header with Status */}
            <div className="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
              <div>
                <span className="text-muted small d-block">Booking Reference</span>
                <span className="h5 fw-bold font-monospace text-primary mb-0">
                  {booking.bookingReference}
                </span>
              </div>
              <div>
                {booking.status === 'CONFIRMED' ? (
                  <span className="badge bg-success-subtle text-success border border-success-subtle px-3 py-2 fs-6">
                    ● CONFIRMED
                  </span>
                ) : (
                  <span className="badge bg-danger-subtle text-danger border border-danger-subtle px-3 py-2 fs-6">
                    ● CANCELLED
                  </span>
                )}
              </div>
            </div>

            <div className="card-body p-4">
              <div className="row g-4 mb-4">
                <div className="col-md-6">
                  <h6 className="fw-bold text-dark mb-1">{booking.hotelName}</h6>
                  <p className="text-muted small mb-2">📍 {booking.hotelLocation}</p>
                  <p className="small mb-0">
                    <strong>Room Category:</strong> {booking.roomName}
                  </p>
                </div>
                <div className="col-md-6 text-md-end">
                  <span className="text-muted small d-block">Guest Contact</span>
                  <span className="fw-bold text-dark d-block">{booking.customerName}</span>
                  <span className="text-muted small d-block">{booking.email}</span>
                  <span className="text-muted small d-block">{booking.phone}</span>
                </div>
              </div>

              {/* Stay Itinerary Box */}
              <div className="p-3 bg-light rounded border mb-4">
                <div className="row g-2 text-center">
                  <div className="col-4 border-end">
                    <span className="text-muted small d-block" style={{ fontSize: '0.75rem' }}>CHECK-IN</span>
                    <span className="fw-bold text-dark small">{formatDateDisplay(booking.checkIn)}</span>
                  </div>
                  <div className="col-4 border-end">
                    <span className="text-muted small d-block" style={{ fontSize: '0.75rem' }}>CHECK-OUT</span>
                    <span className="fw-bold text-dark small">{formatDateDisplay(booking.checkOut)}</span>
                  </div>
                  <div className="col-4">
                    <span className="text-muted small d-block" style={{ fontSize: '0.75rem' }}>STAY LENGTH</span>
                    <span className="fw-bold text-dark small">{booking.nights} {booking.nights === 1 ? 'Night' : 'Nights'}</span>
                  </div>
                </div>
              </div>

              {/* Pricing Breakdown */}
              <div className="d-flex justify-content-between align-items-center pt-2 border-top mb-4">
                <div>
                  <span className="text-muted small d-block">Historical Rate Locked:</span>
                  <span className="text-muted small">{formatCurrency(booking.pricePerNight)} &times; {booking.nights} nights</span>
                </div>
                <div className="text-end">
                  <span className="text-muted small d-block">Total Booking Value:</span>
                  <span className="h4 fw-bold text-dark mb-0">{formatCurrency(booking.totalAmount)}</span>
                </div>
              </div>

              {/* Actions Footer */}
              <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 pt-3 border-top">
                <Link
                  to={`/confirmation/${encodeURIComponent(booking.bookingReference)}`}
                  state={{ booking }}
                  className="btn btn-tt-outline btn-sm"
                >
                  📮 View Destination Postcard
                </Link>

                {booking.status === 'CONFIRMED' ? (
                  <button
                    type="button"
                    className="btn btn-outline-danger btn-sm"
                    onClick={() => setShowCancelModal(true)}
                  >
                    Cancel This Booking
                  </button>
                ) : (
                  <span className="text-muted small fst-italic">
                    This reservation has been cancelled.
                  </span>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Cancel Confirmation Modal */}
        {showCancelModal && (
          <div
            className="modal fade show d-block"
            tabIndex="-1"
            style={{ backgroundColor: 'rgba(15, 23, 42, 0.5)' }}
          >
            <div className="modal-dialog modal-dialog-centered">
              <div className="modal-content border-0 shadow-lg">
                <div className="modal-header bg-danger text-white">
                  <h5 className="modal-title h6 fw-bold">Confirm Booking Cancellation</h5>
                  <button
                    type="button"
                    className="btn-close btn-close-white"
                    onClick={() => setShowCancelModal(false)}
                    disabled={cancelling}
                  ></button>
                </div>
                <div className="modal-body p-4">
                  <p className="mb-2">
                    Are you sure you want to cancel your stay at <strong>{booking?.hotelName}</strong>?
                  </p>
                  <p className="text-muted small mb-3">
                    Dates: {formatDateDisplay(booking?.checkIn)} to {formatDateDisplay(booking?.checkOut)} ({booking?.nights} nights).
                  </p>
                  <div className="alert alert-warning py-2 px-3 small mb-0">
                    ⚠️ This action cannot be reversed. The room inventory will immediately become available to other guests.
                  </div>
                </div>
                <div className="modal-footer bg-light">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => setShowCancelModal(false)}
                    disabled={cancelling}
                  >
                    Keep Booking
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    onClick={handleCancelBooking}
                    disabled={cancelling}
                  >
                    {cancelling ? (
                      <>
                        <span className="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                        Cancelling...
                      </>
                    ) : (
                      'Yes, Cancel Booking'
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
