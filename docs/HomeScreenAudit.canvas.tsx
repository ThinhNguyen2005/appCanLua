import React, { useState } from 'react';

// Types for Audit Matrix
interface ComponentAudit {
  id: string;
  name: string;
  file: string;
  importance: 'P0' | 'P1' | 'P2';
  suitabilityScore: number; // /10
  designTokensScore: number; // /10
  ergonomicsScore: number; // /10
  currentDesign: string;
  identifiedIssues: string[];
  logicFlaws: string[];
  recommendedFix: string;
  codeSnippet?: string;
}

export default function HomeScreenUxAudit() {
  const [activeTab, setActiveTab] = useState<'overview' | 'scorecard' | 'tokens' | 'logic' | 'roadmap'>('overview');
  const [filterPriority, setFilterPriority] = useState<string>('ALL');

  const components: ComponentAudit[] = [
    {
      id: 'summary-card',
      name: 'Thẻ Tóm Tắt Ngày (CardListSummaryCard)',
      file: 'CardListSummaryCard.kt',
      importance: 'P0',
      suitabilityScore: 7,
      designTokensScore: 5,
      ergonomicsScore: 8,
      currentDesign: 'Card viền mỏng 1dp, elevation 0dp, thanh màu xanh accent 4dp trên đỉnh, bo góc cứng 16.dp.',
      identifiedIssues: [
        'Bo góc hardcoded 16.dp không theo token chuẩn AppDimensions (Md: 12dp, Lg: 20dp).',
        'Khoảng cách dòng 14.dp lẻ, phá vỡ thang 4dp (SpacingSm: 12dp, SpacingMd: 16dp).',
        'Thanh accent xanh 4.dp trên đỉnh tạo cảm giác bị cắt vụn thay vì đổ bóng phân cấp (Material 3 tonal elevation).',
        'Elevation = 0dp phẳng làm thẻ tóm tắt chìm hơn các CardItem bên dưới (CardItem có elevation 2dp).'
      ],
      logicFlaws: [
        'Bẫy Reset Lúc Nửa Đêm (Midnight Reset): Chỉ lọc `todayCards`. Sang ngày mới (0h01), tóm tắt về 0 dù nông dân còn hàng chục phiếu chưa chốt cân.',
        'Thiếu bộ chuyển đổi phạm vi (Scope Selector): Không thể xem nhanh "Vụ Này" hoặc "Tháng Này".',
        'Chưa cá nhân hóa theo vai trò: Thương lái cần xem "Sản lượng thu mua & Tổng tiền chi", Nông dân cần xem "Sản lượng đã cân & Tiền còn nợ".'
      ],
      recommendedFix: 'Chuyển sang ElevatedCard hoặc Card với tonal elevation 1dp + border tinh tế; áp dụng AppDimensions.CornerRadiusLg (20dp); thêm nút toggle [Hôm nay | Vụ này].'
    },
    {
      id: 'filter-bar',
      name: 'Thanh Bộ Lọc (CardListFilterBar & Sheet)',
      file: 'CardListFilterBar.kt / CardListFilterSheet.kt',
      importance: 'P1',
      suitabilityScore: 6.5,
      designTokensScore: 8,
      ergonomicsScore: 6,
      currentDesign: 'Row cuộn ngang gồm AssistChip mở FilterSheet + các FilterChip đã chọn có nút X.',
      identifiedIssues: [
        'Nút "X" (clear filter) trong FilterChip quá nhỏ (<32dp), nông dân tay ướt/bám bùn ngoài đồng rất khó chạm chính xác.',
        'Cuộn ngang (horizontal scroll) dễ xung đột với thao tác vuốt cạnh back của Android gesture navigation.',
        'Khoảng cách giữa các chip 8.dp sát nhau, dễ chạm nhầm khi đứng trên bờ ruộng.'
      ],
      logicFlaws: [
        'THIẾU TÍNH NĂNG TÌM KIẾM (Search): Thương lái có 50-100 nông dân không thể gõ tên tìm kiếm nhanh, phải cuộn tìm thủ công.',
        'Thiếu lọc theo trạng thái tài chính: Không thể lọc nhanh "Phiếu còn nợ tiền" hoặc "Phiếu đã xong".',
        'Bị ẩn hoàn toàn khi chưa có danh sách mùa vụ / giống lúa.'
      ],
      recommendedFix: 'Tích hợp thanh SearchBar chuẩn M3 lên đầu màn hình; bổ sung filter nhanh trạng thái thanh toán [Tất cả | Chưa xong | Đã thanh toán]; tăng touch target nút X lên >= 48dp.'
    },
    {
      id: 'date-headers',
      name: 'Tiêu Đề Nhóm Ngày (Grouped Date Header)',
      file: 'CardListScreen.kt (item header_$date)',
      importance: 'P1',
      suitabilityScore: 8.5,
      designTokensScore: 9,
      ergonomicsScore: 7,
      currentDesign: 'Row chứa Text ngày (Hôm nay / Hôm qua / Ngày dd/MM/yyyy) + Badge số lượng phiếu trong ngày.',
      identifiedIssues: [
        'Là item cuộn thông thường (non-sticky). Khi danh sách dài 30-50 phiếu, tiêu đề trôi mất, người dùng mất phương hướng về mốc thời gian.',
        'Contrast badge ngày (SurfaceContainer + TextSecondary) dưới ánh nắng gắt miền Tây hơi mờ.'
      ],
      logicFlaws: [
        'Chưa hiển thị tổng khối lượng (kg) hoặc tổng tiền gom theo từng ngày riêng biệt để đối chiếu sổ sách cuối ngày.'
      ],
      recommendedFix: 'Chuyển sang `stickyHeader { ... }` của Compose LazyColumn; bổ sung tổng kg tóm tắt ngay cạnh badge số lượng phiếu.'
    },
    {
      id: 'card-item',
      name: 'Thẻ Phiếu Cân (CardItem)',
      file: 'CardItem.kt',
      importance: 'P0',
      suitabilityScore: 7.5,
      designTokensScore: 6,
      ergonomicsScore: 8,
      currentDesign: 'Card clickable, shape 16.dp, elevation 2.dp. Đổi nền xanh GreenSurface khi đã thanh toán, nền xám LockedBg khi khóa.',
      identifiedIssues: [
        'Bo góc hardcoded 16.dp, không dùng AppDimensions token.',
        'Vi phạm quy tắc bo góc đồng tâm (Concentric Radii): Thẻ ngoài 16dp, padding 16dp, nhưng badge bên trong bo 8dp (gây lệch cung tròn thị giác).',
        'Ô nhiễm màu xanh (Green Overload): Khi phiếu đã trả tiền (isPaid), cả container đổi màu GreenSurface, đè lên các tag xanh bên trong, làm mất điểm nhấn phân cấp.',
        'Chữ số kg và tiền tệ cần độ tương phản cao hơn dưới ánh sáng mặt trời ngoài trời.'
      ],
      logicFlaws: [
        'LỖI LOGIC NGUY HIỂM Ở SWIPE-TO-DELETE: Code chỉ cho phép vuốt xóa khi `!card.isLocked && card.isPaid`. Nếu tạo nhầm phiếu test chưa thanh toán, vuốt không phản hồi, gây ức chế.',
        'Thiếu CTA thao tác nhanh: Muốn vào cân tiếp phải click vào thẻ -> vào Detail -> bấm Cân tiếp (tốn 2 bước).'
      ],
      recommendedFix: 'Sửa điều kiện xóa phiếu (cho phép xóa phiếu nháp kèm xác nhận); thêm nút tắt "Vào cân" trực tiếp trên thẻ; dùng viền trạng thái (Status Indicator border/dot) thay vì nhuộm xanh toàn bộ nền card.'
    },
    {
      id: 'fab-create',
      name: 'Nút Tạo Phiếu Nổi (CardListCreateFab)',
      file: 'CardListCreateFab.kt',
      importance: 'P0',
      suitabilityScore: 8,
      designTokensScore: 7.5,
      ergonomicsScore: 8.5,
      currentDesign: 'ExtendedFloatingActionButton xanh lá, bo góc 12.dp (CornerRadiusMd), ẩn khi cuộn xuống và hiện khi cuộn lên.',
      identifiedIssues: [
        'Extended FAB bo góc 12.dp hơi gắt (M3 khuyến nghị 16dp hoặc full pill 28dp cho Extended FAB).',
        'Icon thiếu contentDescription cho TalkBack (đang để TODO trong code).',
        'Đồng bộ ẩn BottomBarVisibility khi cuộn xuống tạo ra khoảng trống đáy 96dp bị bỏ phí trong LazyColumn.'
      ],
      logicFlaws: [
        'Không hiển thị hạn mức thẻ miễn phí còn lại (Quota indicator) trực tiếp trên nút, chỉ hiện dialog cảnh báo khi người dùng đã bấm vào.'
      ],
      recommendedFix: 'Bo góc 16dp / pill cho FAB; thêm badge quota nhỏ (ví dụ "3/5 thẻ"); bổ sung accessibility contentDescription.'
    },
    {
      id: 'empty-skeleton',
      name: 'Trạng Thái Rỗng & Tải (Empty State & Skeleton)',
      file: 'CardListEmptyState.kt / CardListSkeleton.kt',
      importance: 'P2',
      suitabilityScore: 9,
      designTokensScore: 9,
      ergonomicsScore: 9,
      currentDesign: 'Icon cân tròn, văn bản hướng dẫn rõ ràng, nút CTA tạo phiếu hoặc đồng bộ lại; Skeleton shimmer 3 thẻ khi đang nạp.',
      identifiedIssues: [
        'Nút bấm "Tạo phiếu đầu tiên" dùng Button full width ở màn hình lớn có thể quá dài, cần giới hạn AppDimensions.FormMaxWidth.'
      ],
      logicFlaws: [
        'Đã xử lý rất tốt: Phân tách rõ trường hợp "Chưa có dữ liệu" và "Không tìm thấy kết quả theo bộ lọc".'
      ],
      recommendedFix: 'Giữ nguyên kiến trúc; bổ sung minh họa đồ họa sinh động hơn về bông lúa / bao lúa thay vì icon đơn thuần.'
    }
  ];

  const filteredComponents = filterPriority === 'ALL' 
    ? components 
    : components.filter(c => c.importance === filterPriority);

  return (
    <div style={{ fontFamily: 'system-ui, -apple-system, sans-serif', backgroundColor: '#0F172A', color: '#F8FAFC', padding: '24px', minHeight: '100vh' }}>
      {/* Header */}
      <div style={{ borderBottom: '1px solid #334155', paddingBottom: '20px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <span style={{ backgroundColor: '#10B981', color: '#064E3B', padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: '700', textTransform: 'uppercase' }}>
              UX & Ergonomics Audit
            </span>
            <h1 style={{ fontSize: '26px', fontWeight: '800', margin: '8px 0 4px 0', color: '#FFFFFF' }}>
              Báo Cáo Kiểm Toán UX/UI Trang Chủ (HomeScreen / CardListScreen)
            </h1>
            <p style={{ margin: 0, color: '#94A3B8', fontSize: '14px' }}>
              Ứng dụng: <strong>Cân Lúa (Android Jetpack Compose & Material 3)</strong> | Đối tượng: <strong>Nông dân & Thương lái Miền Tây</strong>
            </p>
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <span style={{ backgroundColor: '#1E293B', border: '1px solid #475569', padding: '6px 12px', borderRadius: '8px', fontSize: '13px' }}>
              📱 Framework: <strong>Jetpack Compose</strong>
            </span>
            <span style={{ backgroundColor: '#1E293B', border: '1px solid #475569', padding: '6px 12px', borderRadius: '8px', fontSize: '13px' }}>
              🎯 Target: <strong>Touch & Outdoor Readability</strong>
            </span>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div style={{ display: 'flex', gap: '8px', marginTop: '20px', overflowX: 'auto', paddingBottom: '4px' }}>
          {[
            { id: 'overview', label: '📊 Tổng Quan & Radar Điểm' },
            { id: 'scorecard', label: '📋 Bảng Phân Tích Chi Tiết (Scorecard)' },
            { id: 'tokens', label: '🎨 Thiết Kế: Bo Góc & Đổ Bóng (M3)' },
            { id: 'logic', label: '⚡ Logic Nghiệp Vụ Thừa/Thiếu' },
            { id: 'roadmap', label: '🚀 Lộ Trình Cải Tiến (Action Plan)' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              style={{
                backgroundColor: activeTab === tab.id ? '#10B981' : '#1E293B',
                color: activeTab === tab.id ? '#064E3B' : '#CBD5E1',
                border: 'none',
                padding: '8px 16px',
                borderRadius: '8px',
                fontSize: '13px',
                fontWeight: '600',
                cursor: 'pointer',
                transition: 'all 0.2s',
                whiteSpace: 'nowrap'
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* TAB 1: OVERVIEW */}
      {activeTab === 'overview' && (
        <div>
          {/* Key Metrics Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
            {[
              { label: 'Điểm UX Tổng Thể', value: '7.4 / 10', badge: 'Khá Tốt', color: '#10B981' },
              { label: 'Độ Chuẩn Design Tokens', value: '6.2 / 10', badge: 'Cần Đồng Bộ', color: '#F59E0B' },
              { label: 'Công Thái Học Cảm Ứng (Touch)', value: '7.8 / 10', badge: 'Tốt', color: '#10B981' },
              { label: 'Hiển Thị Ngoài Trời Nắng', value: '6.5 / 10', badge: 'Cần Tăng Contrast', color: '#F59E0B' },
              { label: 'Lỗi Logic Nghiêm Trọng', value: '2 Vấn Đề', badge: 'Cần Vá Ngay', color: '#EF4444' }
            ].map((metric, idx) => (
              <div key={idx} style={{ backgroundColor: '#1E293B', border: '1px solid #334155', borderRadius: '12px', padding: '16px' }}>
                <span style={{ fontSize: '12px', color: '#94A3B8' }}>{metric.label}</span>
                <div style={{ fontSize: '22px', fontWeight: '800', margin: '6px 0', color: metric.color }}>{metric.value}</div>
                <span style={{ fontSize: '11px', backgroundColor: `${metric.color}22`, color: metric.color, padding: '2px 8px', borderRadius: '6px', fontWeight: '600' }}>
                  {metric.badge}
                </span>
              </div>
            ))}
          </div>

          {/* Context & Persona Insight */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px' }}>
            <div style={{ backgroundColor: '#1E293B', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#38BDF8', marginTop: 0 }}>
                🌾 Đặc Thù Người Dùng Ngoài Đồng Ruộng
              </h3>
              <ul style={{ paddingLeft: '20px', color: '#CBD5E1', fontSize: '13px', lineHeight: '1.8', margin: 0 }}>
                <li><strong>Môi trường nắng gắt (&gt;50,000 Lux):</strong> Màu chữ xám nhạt (`TextHint`), viền mờ 0.5dp hoàn toàn vô hình dưới nắng trưa. Cần contrast tối thiểu 4.5:1 (WCAG AA) và số liệu to bản.</li>
                <li><strong>Tay dính nước / bùn / đeo găng:</strong> Vùng chạm (Touch Target) bắt buộc phải đạt &gt;= 48dp theo Fitts's Law. Nút xóa nhỏ trong chip là điểm nghẽn dễ gây chạm trượt.</li>
                <li><strong>Thao tác một tay khi cầm bao lúa:</strong> Tất cả nút bấm chính (Tạo phiếu, Vào cân) phải nằm trong "Thumb Zone" (1/3 dưới màn hình).</li>
              </ul>
            </div>

            <div style={{ backgroundColor: '#1E293B', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
              <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#F472B6', marginTop: 0 }}>
                ⚠️ 3 Nghịch Lý Thiết Kế Lớn Nhất Trên HomeScreen
              </h3>
              <ol style={{ paddingLeft: '20px', color: '#CBD5E1', fontSize: '13px', lineHeight: '1.8', margin: 0 }}>
                <li><strong>"Bẫy Reset Nửa Đêm":</strong> Thẻ tóm tắt chỉ cộng dồn phiếu của ngày hôm nay. Sang ngày mới, toàn bộ tổng sản lượng biến mất thành 0 dù mùa gặt vẫn đang tiếp diễn.</li>
                <li><strong>Nghịch đảo phân cấp thị giác (Elevation Inversion):</strong> Thẻ tổng kết đầu trang để phẳng `0dp`, trong khi các thẻ con bên dưới lại nổi `2dp`.</li>
                <li><strong>Khóa vuốt xóa phi lý:</strong> Chỉ cho vuốt xóa khi `card.isPaid = true`. Phiếu rác/tạo thử chưa trả tiền thì không vuốt xóa được!</li>
              </ol>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: SCORECARD */}
      {activeTab === 'scorecard' && (
        <div>
          {/* Filter Bar */}
          <div style={{ display: 'flex', gap: '8px', marginBottom: '16px', alignItems: 'center' }}>
            <span style={{ fontSize: '13px', color: '#94A3B8' }}>Lọc theo độ ưu tiên:</span>
            {['ALL', 'P0', 'P1', 'P2'].map(p => (
              <button
                key={p}
                onClick={() => setFilterPriority(p)}
                style={{
                  backgroundColor: filterPriority === p ? '#38BDF8' : '#1E293B',
                  color: filterPriority === p ? '#0F172A' : '#CBD5E1',
                  border: '1px solid #475569',
                  padding: '4px 12px',
                  borderRadius: '6px',
                  fontSize: '12px',
                  fontWeight: '600',
                  cursor: 'pointer'
                }}
              >
                {p === 'ALL' ? 'Tất cả thành phần' : `Ưu tiên ${p}`}
              </button>
            ))}
          </div>

          {/* Table */}
          <div style={{ overflowX: 'auto', border: '1px solid #334155', borderRadius: '12px' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr style={{ backgroundColor: '#1E293B', borderBottom: '1px solid #334155' }}>
                  <th style={{ padding: '12px 16px', color: '#94A3B8' }}>Thành Phần</th>
                  <th style={{ padding: '12px 16px', color: '#94A3B8' }}>Ưu Tiên</th>
                  <th style={{ padding: '12px 16px', color: '#94A3B8' }}>Đánh Giá</th>
                  <th style={{ padding: '12px 16px', color: '#94A3B8' }}>Vấn Đề UI / Tokens</th>
                  <th style={{ padding: '12px 16px', color: '#94A3B8' }}>Lỗ Hổng Logic</th>
                  <th style={{ padding: '12px 16px', color: '#94A3B8' }}>Giải Pháp Đề Xuất</th>
                </tr>
              </thead>
              <tbody>
                {filteredComponents.map((c, idx) => (
                  <tr key={c.id} style={{ borderBottom: '1px solid #334155', backgroundColor: idx % 2 === 0 ? '#0F172A' : '#131F37' }}>
                    <td style={{ padding: '14px 16px', fontWeight: '700', color: '#FFFFFF', minWidth: '180px' }}>
                      {c.name}
                      <div style={{ fontSize: '11px', color: '#64748B', fontWeight: '400', marginTop: '2px' }}>{c.file}</div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      <span style={{
                        padding: '2px 8px',
                        borderRadius: '6px',
                        fontSize: '11px',
                        fontWeight: '700',
                        backgroundColor: c.importance === 'P0' ? '#EF444422' : c.importance === 'P1' ? '#F59E0B22' : '#10B98122',
                        color: c.importance === 'P0' ? '#EF4444' : c.importance === 'P1' ? '#F59E0B' : '#10B981'
                      }}>
                        {c.importance}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px', minWidth: '100px' }}>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
                        <span style={{ fontSize: '11px', color: '#94A3B8' }}>Phù hợp: <strong style={{ color: '#F8FAFC' }}>{c.suitabilityScore}/10</strong></span>
                        <span style={{ fontSize: '11px', color: '#94A3B8' }}>Tokens: <strong style={{ color: '#F8FAFC' }}>{c.designTokensScore}/10</strong></span>
                        <span style={{ fontSize: '11px', color: '#94A3B8' }}>Touch: <strong style={{ color: '#F8FAFC' }}>{c.ergonomicsScore}/10</strong></span>
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px', color: '#CBD5E1', minWidth: '220px' }}>
                      <ul style={{ margin: 0, paddingLeft: '16px' }}>
                        {c.identifiedIssues.map((issue, i) => (
                          <li key={i} style={{ marginBottom: '4px' }}>{issue}</li>
                        ))}
                      </ul>
                    </td>
                    <td style={{ padding: '14px 16px', color: '#FCA5A5', minWidth: '220px' }}>
                      <ul style={{ margin: 0, paddingLeft: '16px' }}>
                        {c.logicFlaws.map((flaw, i) => (
                          <li key={i} style={{ marginBottom: '4px' }}>{flaw}</li>
                        ))}
                      </ul>
                    </td>
                    <td style={{ padding: '14px 16px', color: '#86EFAC', minWidth: '220px' }}>
                      {c.recommendedFix}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: DESIGN TOKENS & ELEVATION */}
      {activeTab === 'tokens' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          <div style={{ backgroundColor: '#1E293B', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#38BDF8', marginTop: 0 }}>
              📐 1. Kiểm Toán Bo Góc & Quy Tắc Đồng Tâm (Concentric Radii)
            </h3>
            <p style={{ fontSize: '13px', color: '#CBD5E1', lineHeight: '1.6' }}>
              Quy tắc vàng của thiết kế giao diện hiện đại: <br />
              <code style={{ backgroundColor: '#0F172A', padding: '2px 6px', borderRadius: '4px', color: '#F59E0B' }}>
                R_trong = max(0, R_ngoài - Padding)
              </code>
            </p>
            <div style={{ backgroundColor: '#0F172A', borderRadius: '8px', padding: '14px', fontSize: '12px', marginTop: '12px' }}>
              <div style={{ color: '#EF4444', marginBottom: '8px' }}>❌ <strong>Hiện trạng vi phạm trong mã nguồn:</strong></div>
              <div>• `CardListSummaryCard`: Bo góc cứng `16.dp` (không nằm trong thang `AppDimensions`).</div>
              <div>• `CardItem`: Container bo `16.dp`, padding `16.dp`, badge bên trong bo `8.dp` (bị lệch cung tròn thị giác).</div>
              <div style={{ color: '#10B981', marginTop: '12px', marginBottom: '8px' }}>✅ <strong>Chuẩn hóa theo Hệ Thống Tokens:</strong></div>
              <div>• Container Card ngoài: <strong>AppDimensions.CornerRadiusLg (20.dp)</strong></div>
              <div>• Padding: <strong>AppDimensions.SpacingMd (16.dp)</strong></div>
              <div>• Element / Badge con bên trong: <strong>AppDimensions.CornerRadiusSm (4-8.dp)</strong></div>
            </div>
          </div>

          <div style={{ backgroundColor: '#1E293B', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#FBBF24', marginTop: 0 }}>
              🌑 2. Phân Cấp Đổ Bóng & Elevation (Material Design 3)
            </h3>
            <p style={{ fontSize: '13px', color: '#CBD5E1', lineHeight: '1.6' }}>
              Material 3 không sử dụng bóng đổ xám cứng ngắc mà dùng <strong>Tonal Elevation</strong> (pha trộn màu tint primary lên bề mặt surface).
            </p>
            <div style={{ backgroundColor: '#0F172A', borderRadius: '8px', padding: '14px', fontSize: '12px', marginTop: '12px' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid #334155' }}>
                    <th style={{ padding: '6px 0', color: '#94A3B8' }}>Lớp Giao Diện</th>
                    <th style={{ padding: '6px 0', color: '#94A3B8' }}>Hiện Tại</th>
                    <th style={{ padding: '6px 0', color: '#94A3B8' }}>Chuẩn M3 Đề Xuất</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td style={{ padding: '6px 0' }}>Nền ứng dụng</td>
                    <td>0dp</td>
                    <td style={{ color: '#38BDF8' }}>Surface (0dp)</td>
                  </tr>
                  <tr>
                    <td style={{ padding: '6px 0' }}>Thẻ Tóm Tắt (Summary)</td>
                    <td style={{ color: '#EF4444' }}>0dp + viền</td>
                    <td style={{ color: '#10B981' }}>ElevatedCard (1dp tonal)</td>
                  </tr>
                  <tr>
                    <td style={{ padding: '6px 0' }}>Thẻ Phiếu Cân (CardItem)</td>
                    <td>2dp shadow</td>
                    <td style={{ color: '#10B981' }}>OutlinedCard (0dp + 1dp border)</td>
                  </tr>
                  <tr>
                    <td style={{ padding: '6px 0' }}>Nút Tạo Phiếu (FAB)</td>
                    <td>4dp / 8dp</td>
                    <td style={{ color: '#10B981' }}>ElevationLevel3 (6dp / 8dp)</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: BUSINESS LOGIC AUDIT */}
      {activeTab === 'logic' && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          <div style={{ backgroundColor: '#1E293B', border: '1px solid #EF4444', borderRadius: '12px', padding: '20px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#EF4444', marginTop: 0 }}>
              🚨 Lỗi Logic 1: Bẫy Reset Nửa Đêm Của Thẻ Tóm Tắt
            </h3>
            <div style={{ fontSize: '13px', color: '#CBD5E1', lineHeight: '1.6' }}>
              <p>
                Đoạn code trong <code>CardListScreenContent</code>:
              </p>
              <pre style={{ backgroundColor: '#0F172A', padding: '10px', borderRadius: '6px', fontSize: '11px', color: '#FCA5A5', overflowX: 'auto' }}>
{`val todayCards = remember(cards, todayDateStr) {
    cards.filter { dateFormat.format(it.date) == todayDateStr }
}
val todayTotalKg = remember(todayCards) { todayCards.sumOf { it.totalWeight } }`}
              </pre>
              <p>
                <strong>Hậu quả nghiệp vụ:</strong> Vụ gặt lúa thường diễn ra liên tục 3-5 ngày. Nông dân cân lúa đến 23h đêm, sáng thức dậy thấy thẻ tóm tắt báo "0 kg - 0 đ", dễ hoang mang tưởng mất dữ liệu hoặc không biết hôm qua đã cân được bao nhiêu.
              </p>
              <p style={{ color: '#86EFAC' }}>
                <strong>Khắc phục:</strong> Bổ sung segmented selector cho phép chọn: [Hôm nay | Vụ hiện tại | 7 ngày qua]. Mặc định nếu hôm nay chưa có phiếu, tự động hiển thị tóm tắt của ngày gần nhất có phát sinh cân lúa.
              </p>
            </div>
          </div>

          <div style={{ backgroundColor: '#1E293B', border: '1px solid #F59E0B', borderRadius: '12px', padding: '20px' }}>
            <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#F59E0B', marginTop: 0 }}>
              ⚠️ Lỗi Logic 2: Khóa Thao Tác Vuốt Xóa Phi Lý
            </h3>
            <div style={{ fontSize: '13px', color: '#CBD5E1', lineHeight: '1.6' }}>
              <p>
                Đoạn code trong <code>CardItem.kt</code>:
              </p>
              <pre style={{ backgroundColor: '#0F172A', padding: '10px', borderRadius: '6px', fontSize: '11px', color: '#FDE68A', overflowX: 'auto' }}>
{`val dismissState = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
        if (value == EndToStart && !card.isLocked && card.isPaid) {
            onDelete(card)
        }
    }
)`}
              </pre>
              <p>
                <strong>Hậu quả nghiệp vụ:</strong> Người dùng chỉ được vuốt xóa khi <code>card.isPaid = true</code>! Nếu thương lái tạo nháp một phiếu cân và nhập sai thông tin (phiếu chưa trả tiền), vuốt sang trái sẽ bị "nảy ngược lại" mà không có bất kỳ thông báo Toast hay hướng dẫn nào.
              </p>
              <p style={{ color: '#86EFAC' }}>
                <strong>Khắc phục:</strong> Cho phép vuốt xóa mọi phiếu chưa bị khóa (<code>!card.isLocked</code>), sau đó hiển thị Modal Dialog xác nhận để bảo vệ dữ liệu (đã có sẵn <code>DeleteCardConfirmDialog</code>).
              </p>
            </div>
          </div>
        </div>
      )}

      {/* TAB 5: ROADMAP */}
      {activeTab === 'roadmap' && (
        <div style={{ backgroundColor: '#1E293B', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
          <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#10B981', marginTop: 0 }}>
            🚀 Lộ Trình Triển Khai Cải Tiến HomeScreen (Action Plan)
          </h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', marginTop: '16px' }}>
            {/* Phase 1 */}
            <div style={{ backgroundColor: '#0F172A', border: '1px solid #10B981', borderRadius: '8px', padding: '16px' }}>
              <span style={{ fontSize: '11px', backgroundColor: '#10B98122', color: '#10B981', padding: '2px 8px', borderRadius: '4px', fontWeight: '700' }}>
                GIAI ĐOẠN 1 (P0 - SỬA LỖI & TOKENS)
              </span>
              <h4 style={{ fontSize: '15px', margin: '10px 0 6px 0', color: '#FFFFFF' }}>Vá Lỗi & Đồng Bộ Design Tokens</h4>
              <ul style={{ fontSize: '12px', color: '#CBD5E1', paddingLeft: '16px', lineHeight: '1.6' }}>
                <li>Sửa điều kiện Swipe-to-delete trong <code>CardItem.kt</code>.</li>
                <li>Thay thế toàn bộ hardcoded <code>16.dp</code> bằng <code>AppDimensions.CornerRadiusLg (20dp)</code>.</li>
                <li>Đồng bộ spacing trên thang 4dp (14dp → 12dp/16dp).</li>
                <li>Thêm contentDescription chuẩn cho FAB.</li>
              </ul>
            </div>

            {/* Phase 2 */}
            <div style={{ backgroundColor: '#0F172A', border: '1px solid #38BDF8', borderRadius: '8px', padding: '16px' }}>
              <span style={{ fontSize: '11px', backgroundColor: '#38BDF822', color: '#38BDF8', padding: '2px 8px', borderRadius: '4px', fontWeight: '700' }}>
                GIAI ĐOẠN 2 (P1 - CÔNG THÁI HỌC)
              </span>
              <h4 style={{ fontSize: '15px', margin: '10px 0 6px 0', color: '#FFFFFF' }}>Nâng Tầm Trải Nghiệm Ngoài Trời</h4>
              <ul style={{ fontSize: '12px', color: '#CBD5E1', paddingLeft: '16px', lineHeight: '1.6' }}>
                <li>Chuyển tiêu đề ngày sang <code>stickyHeader</code> có ghim trên đỉnh.</li>
                <li>Thêm tổng sản lượng (kg) vào từng nhóm ngày.</li>
                <li>Thêm nút chuyển phạm vi tóm tắt [Hôm nay | Vụ này].</li>
                <li>Tăng touch target của các nút Clear Filter lên &gt;= 48dp.</li>
              </ul>
            </div>

            {/* Phase 3 */}
            <div style={{ backgroundColor: '#0F172A', border: '1px solid #A855F7', borderRadius: '8px', padding: '16px' }}>
              <span style={{ fontSize: '11px', backgroundColor: '#A855F722', color: '#A855F7', padding: '2px 8px', borderRadius: '4px', fontWeight: '700' }}>
                GIAI ĐOẠN 3 (P2 - TÍNH NĂNG CAO CẤP)
              </span>
              <h4 style={{ fontSize: '15px', margin: '10px 0 6px 0', color: '#FFFFFF' }}>Tìm Kiếm & Tối Ưu Thao Tác</h4>
              <ul style={{ fontSize: '12px', color: '#CBD5E1', paddingLeft: '16px', lineHeight: '1.6' }}>
                <li>Tích hợp ô tìm kiếm nông dân / thương lái (Search Bar M3).</li>
                <li>Nút tắt "Vào cân tiếp" trực tiếp trên CardItem.</li>
                <li>Tối ưu độ tương phản màu sắc cho chế độ trời nắng (High Contrast Outdoor Mode).</li>
              </ul>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

