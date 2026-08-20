import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
import { PresentationViewer } from '../PresentationViewer';

// Mock API functions
vi.mock('../../api/presentations', () => ({
  getSlideImageUrl: (id, slide) => `/api/presentations/${id}/slides/${slide}/image`,
  changeSlide: vi.fn(),
  startPresentation: vi.fn(),
  stopPresentation: vi.fn(),
}));

describe('PresentationViewer Component', () => {
  const mockPresentation = {
    id: 101,
    fileName: 'test-deck.pptx',
    totalSlides: 5,
    currentSlide: 1,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders placeholder when no presentation object is passed', () => {
    render(<PresentationViewer presentation={null} />);
    expect(screen.getByText(/No presentation active/i)).toBeInTheDocument();
  });

  it('renders presentation title and slide count', () => {
    render(<PresentationViewer presentation={mockPresentation} isOrganizer={true} />);
    expect(screen.getByText('test-deck.pptx')).toBeInTheDocument();
    expect(screen.getByText(/Slide 1 of 5/i)).toBeInTheDocument();
  });
});
