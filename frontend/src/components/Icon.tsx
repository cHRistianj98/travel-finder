import type { ReactNode } from 'react';

export type IconName =
  | 'compass' | 'plane' | 'train' | 'bus' | 'car' | 'arrow-right' | 'arrow-up-right'
  | 'calendar' | 'clock' | 'map-pin' | 'check' | 'sparkles' | 'search' | 'route'
  | 'wallet' | 'info' | 'refresh' | 'chevron-down' | 'chevron-right' | 'sliders'
  | 'navigation' | 'sun' | 'x' | 'alert' | 'ticket' | 'send' | 'flag' | 'hourglass';

const icons: Record<IconName, ReactNode> = {
  compass: <><circle cx="12" cy="12" r="9"/><path d="m16.2 7.8-2.5 5.9-5.9 2.5 2.5-5.9z"/></>,
  plane: <><path d="M17.8 20 12 13.2 6.2 20l-1.7-2 3.4-8.4L2 6.5l1.5-2 7.5 1.7 2.7-4.1a1.7 1.7 0 0 1 2.5-.4 1.7 1.7 0 0 1 .4 2.5l-4.1 2.7L20 14.5l-2 1.5-3.1-5.9z" transform="rotate(28 12 12)"/></>,
  train: <><rect x="5" y="2.5" width="14" height="16" rx="4"/><path d="M5 10.5h14M8 21.5l2-3m6 0 2 3M9 15h.01M15 15h.01"/></>,
  bus: <><rect x="4.5" y="3" width="15" height="16" rx="3"/><path d="M4.5 11.5h15M7.5 19v2M16.5 19v2M8.5 15h.01M15.5 15h.01"/></>,
  car: <><path d="m5 10 1.8-5.2A2 2 0 0 1 8.7 3.5h6.6a2 2 0 0 1 1.9 1.3L19 10M4.5 10h15a2 2 0 0 1 2 2v5h-19v-5a2 2 0 0 1 2-2ZM5 17v2a1.5 1.5 0 0 0 3 0v-2m8 0v2a1.5 1.5 0 0 0 3 0v-2M6.5 13.5h.01m11 0h.01"/></>,
  'arrow-right': <><path d="M4 12h16m-7-7 7 7-7 7"/></>,
  'arrow-up-right': <><path d="M6 18 18 6M7 6h11v11"/></>,
  calendar: <><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></>,
  clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.5 2"/></>,
  'map-pin': <><path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/></>,
  check: <><path d="m5 12 4 4L19 6"/></>,
  sparkles: <><path d="m12 3 1.8 6.2L20 11l-6.2 1.8L12 19l-1.8-6.2L4 11l6.2-1.8L12 3ZM19 18l.6 1.4L21 20l-1.4.6L19 22l-.6-1.4L17 20l1.4-.6L19 18Z"/></>,
  search: <><circle cx="10.5" cy="10.5" r="7"/><path d="m16 16 5 5"/></>,
  route: <><circle cx="6" cy="6" r="2"/><path d="M8 6h8a4 4 0 0 1 0 8H9a4 4 0 0 0 0 8h7"/><circle cx="18" cy="22" r="1"/></>,
  wallet: <><rect x="3" y="6" width="18" height="15" rx="3"/><path d="M3 9V6a3 3 0 0 1 3-3h12m0 12h3m-3 0h-1"/></>,
  info: <><circle cx="12" cy="12" r="9"/><path d="M12 11v5m0-8h.01"/></>,
  refresh: <><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.5 9A7 7 0 0 1 18 6l2 6M4 12l2 6A7 7 0 0 0 18.5 15"/></>,
  'chevron-down': <><path d="m6 9 6 6 6-6"/></>,
  'chevron-right': <><path d="m9 6 6 6-6 6"/></>,
  sliders: <><path d="M4 7h16M4 17h16"/><circle cx="9" cy="7" r="2" fill="currentColor" stroke="none"/><circle cx="15" cy="17" r="2" fill="currentColor" stroke="none"/></>,
  navigation: <><path d="m21 3-6.5 18-3-7.5L4 10.5 21 3Z"/></>,
  sun: <><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></>,
  x: <><path d="M5 5 19 19M19 5 5 19"/></>,
  alert: <><circle cx="12" cy="12" r="9"/><path d="M12 7v6m0 4h.01"/></>,
  ticket: <><path d="M3 8a2 2 0 0 0 0 4v5a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5a2 2 0 0 0 0-4V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v1Z"/><path d="M12 5v2m0 3v4m0 3v2"/></>,
  send: <><path d="m22 2-7 20-4-9-9-4 20-7ZM22 2 11 13"/></>,
  flag: <><path d="M5 21V4m0 0c5-4 9 4 14 0v11c-5 4-9-4-14 0"/></>,
  hourglass: <><path d="M6 2h12m-12 20h12M7 2v5l5 5-5 5v5m10-20v5l-5 5 5 5v5"/></>,
};

export function Icon({ name, size = 20, strokeWidth = 1.8, className = '' }: {
  name: IconName;
  size?: number;
  strokeWidth?: number;
  className?: string;
}) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {icons[name]}
    </svg>
  );
}
