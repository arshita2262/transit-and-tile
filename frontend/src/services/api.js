/**
 * Transit & Tile — Central API service
 *
 * All backend communication goes through this module.
 * During development, the Vite proxy forwards /api/* to http://localhost:8081.
 */

const API_BASE = '/api';

/**
 * Shared fetch helper.
 * - Sets Content-Type to application/json for POST/PATCH.
 * - Throws an error carrying parsed JSON error body when server returns non-2xx,
 *   so callers can read status, code, message, and fieldErrors.
 */
async function apiFetch(path, options = {}) {
  const url = `${API_BASE}${path}`;
  const headers = { 'Content-Type': 'application/json', ...options.headers };

  const response = await fetch(url, { ...options, headers });

  if (!response.ok) {
    let errorBody;
    try {
      errorBody = await response.json();
    } catch {
      errorBody = { message: `HTTP error ${response.status}` };
    }
    const error = new Error(errorBody.message || `HTTP error ${response.status}`);
    error.status = response.status;
    error.body = errorBody;
    error.code = errorBody.code || 'UNKNOWN_ERROR';
    error.fieldErrors = errorBody.fieldErrors || [];
    throw error;
  }

  if (response.status === 204) return null;
  return response.json();
}

/**
 * Get hotels with optional destination filter.
 * @param {string} [location] - Destination search term
 * @param {string} [checkIn] - Check-in date (YYYY-MM-DD)
 * @param {string} [checkOut] - Check-out date (YYYY-MM-DD)
 */
export async function getHotels(location, checkIn, checkOut) {
  const params = new URLSearchParams();
  if (location && location.trim()) params.append('location', location.trim());
  if (checkIn) params.append('checkIn', checkIn);
  if (checkOut) params.append('checkOut', checkOut);

  const query = params.toString();
  return apiFetch(query ? `/hotels?${query}` : '/hotels');
}

/**
 * Get hotel details by ID including room category, amenities, policies, and photos.
 * @param {number|string} hotelId
 */
export async function getHotelDetails(hotelId) {
  return apiFetch(`/hotels/${hotelId}`);
}

/**
 * Recalculate room availability for selected dates.
 * @param {number|string} hotelId
 * @param {string} checkIn - YYYY-MM-DD
 * @param {string} checkOut - YYYY-MM-DD
 */
export async function checkAvailability(hotelId, checkIn, checkOut) {
  return apiFetch(`/hotels/${hotelId}/availability?checkIn=${encodeURIComponent(checkIn)}&checkOut=${encodeURIComponent(checkOut)}`);
}

/**
 * Create a confirmed one-room booking.
 * Note: Price and booking reference are authoritative backend calculations.
 * @param {Object} data - { roomTypeId, customerName, email, phone, checkIn, checkOut }
 */
export async function createBooking(data) {
  return apiFetch('/bookings', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/**
 * Look up a booking securely using reference + customer email.
 * @param {string} bookingReference
 * @param {string} email
 */
export async function lookupBooking(bookingReference, email) {
  return apiFetch('/bookings/lookup', {
    method: 'POST',
    body: JSON.stringify({ bookingReference: bookingReference.trim(), email: email.trim().toLowerCase() }),
  });
}

/**
 * Cancel a booking securely using reference + customer email.
 * @param {string} bookingReference
 * @param {string} email
 */
export async function cancelBooking(bookingReference, email) {
  return apiFetch(`/bookings/${encodeURIComponent(bookingReference.trim())}/cancel`, {
    method: 'PATCH',
    body: JSON.stringify({ email: email.trim().toLowerCase() }),
  });
}

export { apiFetch };
