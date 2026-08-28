import React, { useState, useEffect, useRef } from 'react';
import {
  Search,
  User,
  CreditCard,
  BookOpen,
  MapPin,
  Zap,
  CornerDownLeft,
  X,
  Sparkles,
  Command,
  ArrowRight,
  ShieldCheck,
} from 'lucide-react';
import { searchGlobal, SearchResultCategory, SearchResultItem } from '../api/tellerApi';

interface GlobalSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectAction: (item: SearchResultItem) => void;
}

export const GlobalSearchModal: React.FC<GlobalSearchModalProps> = ({
  isOpen,
  onClose,
  onSelectAction,
}) => {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<SearchResultCategory[]>([]);
  const [activeTab, setActiveTab] = useState<'ALL' | 'ACCOUNTS' | 'CUSTOMERS' | 'POLICIES' | 'BRANCHES' | 'ACTIONS'>('ALL');
  const [selectedIndex, setSelectedIndex] = useState<number>(0);
  const [previewItem, setPreviewItem] = useState<SearchResultItem | null>(null);

  const inputRef = useRef<HTMLInputElement>(null);

  // Debounced search
  useEffect(() => {
    if (!isOpen) return;

    const timer = setTimeout(async () => {
      setLoading(true);
      try {
        const res = await searchGlobal(query);
        setCategories(res.categories || []);
        setSelectedIndex(0);
      } catch (err) {
        console.error('Lỗi tìm kiếm:', err);
      } finally {
        setLoading(false);
      }
    }, 120);

    return () => clearTimeout(timer);
  }, [query, isOpen]);

  // Focus input on open
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => {
        inputRef.current?.focus();
      }, 50);
    } else {
      setQuery('');
      setPreviewItem(null);
    }
  }, [isOpen]);

  // Flattened items based on activeTab
  const filteredCategories = categories.filter((c) => activeTab === 'ALL' || c.type === activeTab);
  const flatItems: SearchResultItem[] = filteredCategories.flatMap((c) => c.items);

  // Sync preview item
  useEffect(() => {
    if (flatItems.length > 0 && selectedIndex < flatItems.length) {
      setPreviewItem(flatItems[selectedIndex]);
    } else {
      setPreviewItem(null);
    }
  }, [selectedIndex, categories, activeTab]);

  // Keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      onClose();
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (flatItems.length > 0) {
        setSelectedIndex((prev) => (prev + 1) % flatItems.length);
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (flatItems.length > 0) {
        setSelectedIndex((prev) => (prev - 1 + flatItems.length) % flatItems.length);
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (flatItems.length > 0 && selectedIndex < flatItems.length) {
        handleItemClick(flatItems[selectedIndex]);
      }
    }
  };

  const handleItemClick = (item: SearchResultItem) => {
    onSelectAction(item);
    onClose();
  };

  if (!isOpen) return null;

  const renderIcon = (iconName: string) => {
    switch (iconName) {
      case 'user':
        return <User size={16} className="text-purple-400" />;
      case 'credit-card':
        return <CreditCard size={16} className="text-emerald-400" />;
      case 'book-open':
        return <BookOpen size={16} className="text-blue-400" />;
      case 'map-pin':
        return <MapPin size={16} className="text-amber-400" />;
      default:
        return <Zap size={16} className="text-amber-400" />;
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(8px)',
        zIndex: 9999,
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'center',
        paddingTop: '80px',
        paddingLeft: '20px',
        paddingRight: '20px',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '860px',
          background: 'linear-gradient(180deg, #181c26 0%, #11141c 100%)',
          borderRadius: '16px',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          boxShadow: '0 25px 60px -15px rgba(0, 0, 0, 0.8), 0 0 40px rgba(59, 130, 246, 0.15)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          maxHeight: '80vh',
        }}
        onKeyDown={handleKeyDown}
      >
        {/* Search Header Bar */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            background: 'rgba(255, 255, 255, 0.02)',
          }}
        >
          <Search size={20} style={{ color: '#60a5fa', flexShrink: 0 }} />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Tìm kiếm tài khoản, CIF, khách hàng, biểu phí hoặc gõ lệnh... (vd: 3456, Lan, nộp tiền)"
            style={{
              flex: 1,
              background: 'transparent',
              border: 'none',
              outline: 'none',
              color: '#ffffff',
              fontSize: '1.05rem',
              fontWeight: 500,
            }}
          />
          {query && (
            <button
              onClick={() => setQuery('')}
              style={{
                background: 'rgba(255, 255, 255, 0.08)',
                border: 'none',
                color: '#9ca3af',
                borderRadius: '50%',
                width: '24px',
                height: '24px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
            >
              <X size={14} />
            </button>
          )}
          <span
            style={{
              background: 'rgba(255, 255, 255, 0.06)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              padding: '3px 8px',
              borderRadius: '6px',
              fontSize: '0.75rem',
              color: '#9ca3af',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            ESC to close
          </span>
        </div>

        {/* Category Tabs */}
        <div
          style={{
            padding: '10px 20px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
            background: 'rgba(0, 0, 0, 0.15)',
            overflowX: 'auto',
          }}
        >
          {[
            { id: 'ALL', label: 'Tất cả kết quả' },
            { id: 'ACCOUNTS', label: 'Tài khoản', icon: CreditCard },
            { id: 'CUSTOMERS', label: 'Khách hàng (CIF)', icon: User },
            { id: 'ACTIONS', label: 'Thao tác nhanh', icon: Zap },
            { id: 'POLICIES', label: 'Quy trình', icon: BookOpen },
            { id: 'BRANCHES', label: 'Chi nhánh', icon: MapPin },
          ].map((tab) => {
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => {
                  setActiveTab(tab.id as any);
                  setSelectedIndex(0);
                }}
                style={{
                  background: active ? 'rgba(59, 130, 246, 0.18)' : 'transparent',
                  border: active ? '1px solid rgba(59, 130, 246, 0.4)' : '1px solid transparent',
                  color: active ? '#60a5fa' : '#9ca3af',
                  padding: '5px 12px',
                  borderRadius: '8px',
                  fontSize: '0.8rem',
                  fontWeight: active ? 600 : 400,
                  cursor: 'pointer',
                  whiteSpace: 'nowrap',
                  transition: 'all 0.15s ease',
                }}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Results Body: Split into List (Left) and Detail Preview (Right) */}
        <div style={{ display: 'flex', flex: 1, minHeight: '340px', maxHeight: '480px', overflow: 'hidden' }}>
          
          {/* Left Column: Search Result Groups */}
          <div
            style={{
              flex: '1 1 58%',
              overflowY: 'auto',
              padding: '12px 16px',
              borderRight: '1px solid rgba(255, 255, 255, 0.08)',
            }}
          >
            {loading && flatItems.length === 0 ? (
              <div style={{ padding: '40px 20px', textAlign: 'center', color: '#9ca3af' }}>
                <Sparkles size={28} className="animate-spin" style={{ margin: '0 auto 12px', color: '#60a5fa' }} />
                <p style={{ fontSize: '0.9rem' }}>Đang tìm kiếm thông tin trong hệ thống ngân hàng...</p>
              </div>
            ) : filteredCategories.length === 0 ? (
              <div style={{ padding: '40px 20px', textAlign: 'center', color: '#6b7280' }}>
                <Search size={32} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
                <p style={{ fontSize: '0.95rem', fontWeight: 500, color: '#e5e7eb' }}>
                  Không tìm thấy kết quả nào cho "{query}"
                </p>
                <p style={{ fontSize: '0.8rem', marginTop: '6px' }}>
                  Thử tìm theo số tài khoản (3456789), mã CIF (CIF-0001842), tên (Lan, An) hoặc thao tác (nộp tiền, rút tiền, tỷ giá).
                </p>
              </div>
            ) : (
              <div>
                {filteredCategories.map((cat) => (
                  <div key={cat.type} style={{ marginBottom: '16px' }}>
                    <div
                      style={{
                        fontSize: '0.72rem',
                        fontWeight: 700,
                        textTransform: 'uppercase',
                        letterSpacing: '0.06em',
                        color: '#60a5fa',
                        padding: '4px 8px',
                        marginBottom: '4px',
                      }}
                    >
                      {cat.title} ({cat.items.length})
                    </div>
                    {cat.items.map((item) => {
                      const itemIdx = flatItems.findIndex((x) => x.id === item.id && x.title === item.title);
                      const isSelected = itemIdx === selectedIndex;
                      return (
                        <div
                          key={item.id + item.title}
                          onClick={() => handleItemClick(item)}
                          onMouseEnter={() => setSelectedIndex(itemIdx)}
                          style={{
                            padding: '10px 12px',
                            borderRadius: '10px',
                            marginBottom: '4px',
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            background: isSelected
                              ? 'linear-gradient(90deg, rgba(59, 130, 246, 0.18) 0%, rgba(139, 92, 246, 0.12) 100%)'
                              : 'transparent',
                            border: isSelected ? '1px solid rgba(59, 130, 246, 0.4)' : '1px solid transparent',
                            transition: 'all 0.1s ease',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', minWidth: 0 }}>
                            <div
                              style={{
                                width: '32px',
                                height: '32px',
                                borderRadius: '8px',
                                background: isSelected ? 'rgba(59, 130, 246, 0.25)' : 'rgba(255, 255, 255, 0.05)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                flexShrink: 0,
                              }}
                            >
                              {renderIcon(item.icon)}
                            </div>
                            <div style={{ minWidth: 0 }}>
                              <div
                                style={{
                                  fontSize: '0.88rem',
                                  fontWeight: 600,
                                  color: isSelected ? '#ffffff' : '#e5e7eb',
                                  whiteSpace: 'nowrap',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                }}
                              >
                                {item.title}
                              </div>
                              <div
                                style={{
                                  fontSize: '0.78rem',
                                  color: isSelected ? '#cbd5e1' : '#9ca3af',
                                  whiteSpace: 'nowrap',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  marginTop: '2px',
                                }}
                              >
                                {item.subtitle}
                              </div>
                            </div>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0, marginLeft: '12px' }}>
                            {item.badge && (
                              <span
                                className={`badge badge-${item.badgeColor || 'neutral'}`}
                                style={{ fontSize: '0.68rem', padding: '2px 8px' }}
                              >
                                {item.badge}
                              </span>
                            )}
                            {isSelected && (
                              <CornerDownLeft size={14} style={{ color: '#60a5fa' }} />
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Right Column: Dynamic Preview Card */}
          <div
            style={{
              flex: '1 1 42%',
              background: 'rgba(0, 0, 0, 0.25)',
              padding: '20px',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              overflowY: 'auto',
            }}
          >
            {previewItem ? (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '14px' }}>
                  <div
                    style={{
                      width: '40px',
                      height: '40px',
                      borderRadius: '10px',
                      background: 'rgba(59, 130, 246, 0.2)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {renderIcon(previewItem.icon)}
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#ffffff' }}>
                      {previewItem.title}
                    </h3>
                    <span className={`badge badge-${previewItem.badgeColor || 'neutral'}`} style={{ fontSize: '0.7rem' }}>
                      {previewItem.badge}
                    </span>
                  </div>
                </div>

                <div
                  style={{
                    background: 'rgba(255, 255, 255, 0.04)',
                    borderRadius: '10px',
                    padding: '14px',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    fontSize: '0.82rem',
                    lineHeight: '1.6',
                    color: '#cbd5e1',
                    marginBottom: '16px',
                  }}
                >
                  <div style={{ marginBottom: '8px' }}>
                    <span style={{ color: '#9ca3af' }}>Chi tiết: </span>
                    <strong style={{ color: '#fff' }}>{previewItem.subtitle}</strong>
                  </div>

                  {previewItem.payload && (
                    <div style={{ marginTop: '10px', paddingTop: '10px', borderTop: '1px solid rgba(255, 255, 255, 0.06)' }}>
                      {previewItem.payload.cif && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                          <span style={{ color: '#9ca3af' }}>Mã CIF:</span>
                          <span style={{ color: '#34d399', fontWeight: 600 }}>{previewItem.payload.cif}</span>
                        </div>
                      )}
                      {previewItem.payload.cccd && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                          <span style={{ color: '#9ca3af' }}>CCCD/CMND:</span>
                          <span style={{ color: '#fff' }}>{previewItem.payload.cccd}</span>
                        </div>
                      )}
                      {previewItem.payload.phone && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                          <span style={{ color: '#9ca3af' }}>Số điện thoại:</span>
                          <span style={{ color: '#fff' }}>{previewItem.payload.phone}</span>
                        </div>
                      )}
                      {previewItem.payload.balance && (
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                          <span style={{ color: '#9ca3af' }}>Số dư:</span>
                          <span style={{ color: '#10b981', fontWeight: 700 }}>{previewItem.payload.balance}</span>
                        </div>
                      )}
                      {previewItem.payload.addr && (
                        <div style={{ marginTop: '4px' }}>
                          <span style={{ color: '#9ca3af' }}>Địa chỉ: </span>
                          <span style={{ color: '#fff' }}>{previewItem.payload.addr}</span>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <div
                  style={{
                    background: 'rgba(59, 130, 246, 0.08)',
                    borderRadius: '8px',
                    padding: '10px 12px',
                    border: '1px solid rgba(59, 130, 246, 0.2)',
                    fontSize: '0.78rem',
                    color: '#93c5fd',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                  }}
                >
                  <Sparkles size={14} style={{ color: '#60a5fa', flexShrink: 0 }} />
                  <span>
                    Hành động: Nhấn <strong>Enter</strong> hoặc click để AI Copilot tự động nạp ngữ cảnh và xử lý.
                  </span>
                </div>
              </div>
            ) : (
              <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#6b7280', textAlign: 'center' }}>
                <div>
                  <Command size={28} style={{ margin: '0 auto 10px', opacity: 0.4 }} />
                  <p style={{ fontSize: '0.85rem' }}>Dùng phím ↑ ↓ để xem trước chi tiết</p>
                </div>
              </div>
            )}

            {previewItem && (
              <button
                className="btn btn-primary"
                onClick={() => handleItemClick(previewItem)}
                style={{
                  width: '100%',
                  marginTop: '16px',
                  justifyContent: 'center',
                  padding: '10px',
                  fontSize: '0.85rem',
                }}
              >
                <span>Thực thi / Mở ngữ cảnh</span>
                <ArrowRight size={15} />
              </button>
            )}
          </div>

        </div>

        {/* Footer Navigation Hints */}
        <div
          style={{
            padding: '10px 20px',
            borderTop: '1px solid rgba(255, 255, 255, 0.06)',
            background: 'rgba(0, 0, 0, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '0.75rem',
            color: '#9ca3af',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <span>
              <kbd style={{ background: 'rgba(255,255,255,0.1)', padding: '2px 5px', borderRadius: '4px', marginRight: '4px' }}>↑</kbd>
              <kbd style={{ background: 'rgba(255,255,255,0.1)', padding: '2px 5px', borderRadius: '4px', marginRight: '4px' }}>↓</kbd>
              Điều hướng
            </span>
            <span>
              <kbd style={{ background: 'rgba(255,255,255,0.1)', padding: '2px 5px', borderRadius: '4px', marginRight: '4px' }}>↵</kbd>
              Chọn mục
            </span>
            <span>
              <kbd style={{ background: 'rgba(255,255,255,0.1)', padding: '2px 5px', borderRadius: '4px', marginRight: '4px' }}>Esc</kbd>
              Đóng
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#10b981' }}>
            <ShieldCheck size={14} />
            <span>Smart Counter Omnibox Engine</span>
          </div>
        </div>

      </div>
    </div>
  );
};
