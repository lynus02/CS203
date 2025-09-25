// Country flag emoji mapping
export const countryFlags: Record<string, string> = {
  "United States": "🇺🇸",
  "Canada": "🇨🇦", 
  "Mexico": "🇲🇽",
  "China": "🇨🇳",
  "Japan": "🇯🇵",
  "South Korea": "🇰🇷",
  "Germany": "🇩🇪",
  "France": "🇫🇷",
  "United Kingdom": "🇬🇧",
  "Italy": "🇮🇹",
  "Spain": "🇪🇸",
  "Australia": "🇦🇺",
  "Singapore": "🇸🇬",
  "Thailand": "🇹🇭",
  "Vietnam": "🇻🇳",
  "India": "🇮🇳",
  "Brazil": "🇧🇷",
  "Chile": "🇨🇱",
  "European Union": "🇪🇺"
};

export function CountryFlag({ country, className = "" }: { country: string; className?: string }) {
  const flag = countryFlags[country];
  
  if (!flag) {
    return <span className={`inline-block w-5 ${className}`}>🏳️</span>;
  }
  
  return <span className={`inline-block w-5 ${className}`}>{flag}</span>;
}

export function getCountryWithFlag(country: string): string {
  const flag = countryFlags[country];
  return flag ? `${flag} ${country}` : `🏳️ ${country}`;
}