/**
 * Curated destination details for Destination Postcard Confirmation.
 * Weather is explicitly illustrative sample data (not a live forecast).
 */

export const DESTINATION_DETAILS = {
  jaipur: {
    name: 'Jaipur',
    tagline: 'The Pink City',
    greeting: 'Padharo Mhare Des — Warm Greetings from the Pink City!',
    illustrativeWeather: {
      temp: '26°C',
      condition: 'Sunny & Pleasant',
      icon: '☀️',
      note: 'Typical winter daytime temperature',
    },
    curatedAttraction: {
      name: 'Hawa Mahal & Amber Palace',
      description: 'Marvel at the 953 honeycombed jharokha windows at dawn, then ride up to the regal battlements of Amber Fort.',
    },
    localTip: 'Sample authentic Pyaaz Kachori and Ghewar at Johari Bazaar before sundown.',
    stampLabel: 'ROYAL RAJASTHAN',
    colorAccent: '#ea580c',
  },
  goa: {
    name: 'Goa',
    tagline: 'The Sunlit Pearl of the Coast',
    greeting: 'Sussegado! Sunny Coastal Greetings from Candolim Beach!',
    illustrativeWeather: {
      temp: '29°C',
      condition: 'Breezy & Golden',
      icon: '🌴',
      note: 'Typical coastal sea breeze weather',
    },
    curatedAttraction: {
      name: 'Fort Aguada & Candolim Beach',
      description: 'Catch uninterrupted Arabian Sea sunsets from the 17th-century Portuguese lighthouse.',
    },
    localTip: 'Visit the local beach shacks for fresh Kingfish rava fry and coconut water at 4 PM.',
    stampLabel: 'ARABIAN SEA',
    colorAccent: '#0284c7',
  },
  manali: {
    name: 'Manali',
    tagline: 'Valley of the Gods',
    greeting: 'Ram Ram Ji! Warm Greetings from the Snowcapped Pines of Himachal!',
    illustrativeWeather: {
      temp: '14°C',
      condition: 'Crisp Mountain Breeze',
      icon: '🏔️',
      note: 'Typical cool alpine pine valley weather',
    },
    curatedAttraction: {
      name: 'Solang Valley & Jogini Waterfalls',
      description: 'Hike through fragrant deodar forests to reach the serene cascading tiers of Jogini falls.',
    },
    localTip: 'Grab warm cinnamon rolls and apple crumble from an Old Manali wood-fired bakery.',
    stampLabel: 'HIMALAYAS',
    colorAccent: '#059669',
  },
  munnar: {
    name: 'Munnar',
    tagline: 'Misty Tea Hills of God’s Own Country',
    greeting: 'Namaskaram! Enchanting Misty Greetings from the Tea Estates!',
    illustrativeWeather: {
      temp: '18°C',
      condition: 'Cool & Rolling Mist',
      icon: '🍵',
      note: 'Typical highland morning mist temperature',
    },
    curatedAttraction: {
      name: 'Eravikulam National Park & Lockhart Tea Factory',
      description: 'Spot the rare Nilgiri Tahr along lush green tea slopes shrouded in morning fog.',
    },
    localTip: 'Buy single-origin silver tips white tea fresh from the factory sales counter.',
    stampLabel: 'KERALA HIGHLANDS',
    colorAccent: '#16a34a',
  },
  udaipur: {
    name: 'Udaipur',
    tagline: 'City of Lakes & White Marble',
    greeting: 'Khamma Ghani! Regal Greetings from the Shores of Lake Pichola!',
    illustrativeWeather: {
      temp: '24°C',
      condition: 'Mild & Golden Reflections',
      icon: '🏰',
      note: 'Typical calm lake breeze weather',
    },
    curatedAttraction: {
      name: 'City Palace & Jag Mandir Island',
      description: 'Take a twilight boat ride across Lake Pichola as the marble palace lights illuminate the waters.',
    },
    localTip: 'Watch the Dharohar folk dance at Bagore Ki Haveli around 7 PM.',
    stampLabel: 'MEWAR LAKES',
    colorAccent: '#7c3aed',
  },
};

export const GENERIC_DESTINATION = {
  name: 'Destination Retreat',
  tagline: 'Your Handcrafted Escape',
  greeting: 'Warm Greetings from your Transit & Tile Destination!',
  illustrativeWeather: {
    temp: '25°C',
    condition: 'Pleasant & Welcoming',
    icon: '🌤️',
    note: 'Illustrative seasonal sample reading',
  },
  curatedAttraction: {
    name: 'Historic Town Centre & Scenic Promenade',
    description: 'Explore charming local lanes, historic squares, and authentic architecture on foot.',
  },
  localTip: 'Ask the hotel front desk for their favourite neighborhood tea stall and sweet shop.',
  stampLabel: 'TRANSIT & TILE',
  colorAccent: '#0284c7',
};

/**
 * Resolves destination information from hotel location or name.
 */
export function getDestinationDetails(location = '') {
  if (!location) return GENERIC_DESTINATION;
  const lower = location.toLowerCase();
  for (const [key, details] of Object.entries(DESTINATION_DETAILS)) {
    if (lower.includes(key)) {
      return details;
    }
  }
  return GENERIC_DESTINATION;
}
