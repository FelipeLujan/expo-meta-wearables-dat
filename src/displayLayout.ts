import type {
  DisplayButtonStyle,
  DisplayContentNode,
  DisplayFlexBoxProps,
  DisplayIconStyle,
  DisplayImageSizePreset,
  DisplayTextColor,
  DisplayTextStyle,
} from "./EMWDAT.types";

/** Ergonomic builders for Meta Ray-Ban Display JSON trees (not React — compiles to native DSL). */
export type FlexProps = Omit<DisplayFlexBoxProps, never>;

export function column(children: DisplayContentNode[], props: FlexProps = {}): DisplayContentNode {
  return { type: "flexBox", direction: "column", ...props, children };
}

export function row(children: DisplayContentNode[], props: FlexProps = {}): DisplayContentNode {
  return { type: "flexBox", direction: "row", ...props, children };
}

/** Card panel using the SDK `.card` background. */
export function card(children: DisplayContentNode[], props: FlexProps = {}): DisplayContentNode {
  return column(children, {
    gap: 8,
    paddingAll: 16,
    background: "card",
    ...props,
  });
}

/** DisplayAccess pattern: card body + action row below (non-card background). */
export function screen(
  cardBody: DisplayContentNode[],
  actions: DisplayContentNode[] = [],
  props: FlexProps = {}
): DisplayContentNode {
  return column(
    [
      card(cardBody, { paddingAll: props.paddingAll ?? 16 }),
      row(actions, {
        gap: 8,
        wrap: true,
        alignment: "center",
        crossAlignment: "center",
      }),
    ],
    { gap: 8, paddingAll: 0, ...props }
  );
}

export function text(
  content: string,
  opts: { style?: DisplayTextStyle; color?: DisplayTextColor } = {}
): DisplayContentNode {
  return { type: "text", content, ...opts };
}

export function button(
  label: string,
  onPressId: string,
  opts: { style?: DisplayButtonStyle; iconName?: string } = {}
): DisplayContentNode {
  return { type: "button", label, onPressId, ...opts };
}

export function image(
  uri: string,
  opts: {
    sizePreset?: DisplayImageSizePreset;
    cornerRadius?: "none" | "small" | "medium";
  } = {}
): DisplayContentNode {
  return { type: "image", uri, ...opts };
}

export function icon(name: string, opts: { style?: DisplayIconStyle } = {}): DisplayContentNode {
  return { type: "icon", name, ...opts };
}

/** Tappable list row: icon/thumbnail + text column (Car Maintenance list style). */
export function listRow(
  title: string,
  subtitle: string | undefined,
  onPressId: string,
  leading?: DisplayContentNode,
  opts: { flexGrow?: number } = {}
): DisplayContentNode {
  const textColumn = column(
    [
      text(title, { style: "body" }),
      ...(subtitle ? [text(subtitle, { style: "meta", color: "secondary" })] : []),
    ],
    { gap: 2, flexGrow: opts.flexGrow ?? 7 }
  );

  const leadingBox = leading ? column([leading], { flexGrow: 1 }) : null;

  return row(leadingBox ? [leadingBox, textColumn] : [textColumn], {
    gap: 12,
    crossAlignment: "center",
    paddingAll: 16,
    onPressId,
  });
}
