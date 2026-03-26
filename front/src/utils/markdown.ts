import MarkdownIt from 'markdown-it'

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
})

type LinkOpenRule = NonNullable<typeof markdown.renderer.rules.link_open>

const defaultLinkOpen: LinkOpenRule =
  markdown.renderer.rules.link_open ||
  ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))

markdown.renderer.rules.link_open = ((tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const targetIndex = token.attrIndex('target')
  if (targetIndex < 0) {
    token.attrPush(['target', '_blank'])
  } else {
    token.attrs![targetIndex][1] = '_blank'
  }

  const relIndex = token.attrIndex('rel')
  if (relIndex < 0) {
    token.attrPush(['rel', 'noopener noreferrer'])
  } else {
    token.attrs![relIndex][1] = 'noopener noreferrer'
  }

  return defaultLinkOpen(tokens, idx, options, env, self)
}) as LinkOpenRule

export function renderMarkdown(source?: string | null) {
  if (!source?.trim()) {
    return ''
  }
  return markdown.render(source)
}
