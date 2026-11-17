<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, watch } from "vue";
import { api } from "@/shared/api";
import { TITRES, TYPES_BUG, typeCourt } from "@/shared/constants";
import { dateHeure } from "@/shared/format";
import type { Bug, Page } from "@/shared/types";
import { surEntree } from "../live";

/* Liste des signalements (GET /api/bugs) : filtres, tri, pagination, détail dépliable. */

const filtres = reactive({ type_bug: "", titre: "", id_utilisateur: "", session_id: "" });
const tri = ref<"timestamp" | "titre" | "type_bug" | "id_utilisateur">("timestamp");
const ordre = ref<"asc" | "desc">("desc");
const page = ref(0);
const TAILLE = 20;

const donnees = ref<Page<Bug> | null>(null);
const chargement = ref(false);
const erreur = ref("");
const ouvert = ref<number | null>(null);
const nouveaux = ref(0);
let requete = 0;

async function charger() {
  const n = ++requete;                         // ignore les réponses arrivées dans le désordre
  chargement.value = true;
  const params: Record<string, string | number> = { tri: tri.value, ordre: ordre.value, page: page.value, taille: TAILLE };
  for (const [k, v] of Object.entries(filtres)) if (v.trim()) params[k] = v.trim();
  try {
    const d = await api.listerBugs(params);
    if (n !== requete) return;
    donnees.value = d;
    erreur.value = "";
    nouveaux.value = 0;
  } catch (e) {
    if (n !== requete) return;
    erreur.value = (e as Error).message;
  } finally {
    if (n === requete) chargement.value = false;
  }
}

let delai = 0;
watch(filtres, () => {
  clearTimeout(delai);
  delai = window.setTimeout(() => { page.value = 0; charger(); }, 250);
});
watch([tri, ordre, page], charger, { immediate: true });

function trier(col: typeof tri.value) {
  if (tri.value === col) ordre.value = ordre.value === "asc" ? "desc" : "asc";
  else { tri.value = col; ordre.value = col === "timestamp" ? "desc" : "asc"; }
  page.value = 0;
}

const stop = surEntree((e) => { if (e.type === "SIGNALEMENT") nouveaux.value += 1; });
onBeforeUnmount(stop);

const pages = () => Math.max(1, Math.ceil((donnees.value?.total ?? 0) / TAILLE));
const COLONNES = [
  { cle: "titre", label: "Bug" },
  { cle: "type_bug", label: "Catégorie" },
  { cle: "id_utilisateur", label: "Pseudonyme" },
  { cle: null, label: "Description" },
  { cle: "timestamp", label: "Date" },
] as const;
</script>

<template>
  <div class="bugs">
    <header class="entete">
      <div>
        <h1>Signalements</h1>
        <p class="muted">{{ donnees ? `${donnees.total} signalement${donnees.total > 1 ? "s" : ""}` : "Chargement…" }}</p>
      </div>
      <Transition name="rise">
        <button v-if="nouveaux" type="button" class="btn btn-primary nouveaux" @click="page = 0; charger()">
          ↑ {{ nouveaux }} nouveau{{ nouveaux > 1 ? "x" : "" }}
        </button>
      </Transition>
    </header>

    <div class="card filtres">
      <label class="field"><span>Catégorie</span>
        <select v-model="filtres.type_bug"><option value="">Toutes</option><option v-for="t in TYPES_BUG" :key="t.value" :value="t.value">{{ t.court }}</option></select>
      </label>
      <label class="field"><span>Bug</span>
        <select v-model="filtres.titre"><option value="">Tous</option><option v-for="t in TITRES" :key="t.value" :value="t.value">{{ t.value }} · {{ t.label }}</option></select>
      </label>
      <label class="field"><span>Pseudonyme</span><input v-model="filtres.id_utilisateur" placeholder="Storm_X9" maxlength="30"></label>
      <label class="field"><span>Session</span><input v-model="filtres.session_id" type="number" min="1" placeholder="42"></label>
    </div>

    <p v-if="erreur" class="alert err">{{ erreur }}</p>

    <div class="card table" :class="{ charge: chargement && donnees }">
      <table>
        <thead>
          <tr>
            <th v-for="c in COLONNES" :key="c.label" scope="col" :aria-sort="c.cle && tri === c.cle ? (ordre === 'asc' ? 'ascending' : 'descending') : undefined">
              <button v-if="c.cle" type="button" class="tri" :class="{ on: tri === c.cle }" @click="trier(c.cle)">
                {{ c.label }} <span aria-hidden="true" class="fleche" :class="ordre">{{ tri === c.cle ? "↓" : "↕" }}</span>
              </button>
              <template v-else>{{ c.label }}</template>
            </th>
          </tr>
        </thead>
        <tbody v-if="!donnees">
          <tr v-for="i in 6" :key="i"><td colspan="5"><div class="skeleton" style="height: 1.3rem" /></td></tr>
        </tbody>
        <tbody v-else-if="!donnees.resultats.length">
          <tr><td colspan="5" class="vide">Aucun signalement ne correspond à ces filtres.</td></tr>
        </tbody>
        <TransitionGroup v-else tag="tbody" name="ligne">
          <template v-for="(b, i) in donnees.resultats" :key="b.id">
            <tr class="ligne" :class="{ ouvert: ouvert === b.id }" :style="{ '--i': i }">
              <td>
                <button type="button" class="deplier" :aria-expanded="ouvert === b.id" @click="ouvert = ouvert === b.id ? null : b.id">
                  <span class="chevron" aria-hidden="true">›</span> <b>{{ b.titre }}</b>
                </button>
              </td>
              <td><span class="chip">{{ typeCourt(b.type_bug) }}</span></td>
              <td>{{ b.id_utilisateur }}</td>
              <td class="desc">{{ b.description || "—" }}</td>
              <td class="date mono">{{ dateHeure(b.timestamp) }}</td>
            </tr>
            <tr v-if="ouvert === b.id" :key="`d${b.id}`" class="detail">
              <td colspan="5">
                <div class="meta">
                  <span>Bug n° <b>{{ b.id }}</b></span>
                  <span>Session <b>{{ b.session_id }}</b></span>
                  <span>Affichage <b>{{ b.affichage_id ?? "—" }}</b></span>
                  <span>Apparition couverte <b>{{ b.apparition_id ?? "aucune" }}</b></span>
                  <RouterLink :to="{ path: '/suivi', query: { session: b.session_id } }">Voir la chronologie →</RouterLink>
                </div>
                <pre>{{ JSON.stringify(b.payload, null, 2) }}</pre>
              </td>
            </tr>
          </template>
        </TransitionGroup>
      </table>
    </div>

    <nav class="pagination" aria-label="Pagination">
      <button type="button" class="btn btn-sm" :disabled="page === 0" @click="page--">← Précédent</button>
      <span class="muted">Page {{ page + 1 }} / {{ pages() }}</span>
      <button type="button" class="btn btn-sm" :disabled="page + 1 >= pages()" @click="page++">Suivant →</button>
    </nav>
  </div>
</template>

<style scoped>
.bugs { display: grid; gap: 1.1rem; }
.entete { display: flex; justify-content: space-between; align-items: flex-end; gap: 1rem; }
.entete p { margin: 0.3rem 0 0; }
.nouveaux { background: var(--accent); border-color: var(--accent); color: #fff; }
.filtres { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 0.9rem; padding: 1rem 1.15rem; }

.table { overflow-x: auto; transition: opacity 0.2s; }
.table.charge { opacity: 0.6; }
table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
th, td { padding: 0.65rem 0.9rem; text-align: left; border-bottom: 1px solid var(--line); }
thead th { font-size: 0.8125rem; color: var(--ink-2); font-weight: 600; white-space: nowrap; border-bottom-color: var(--line-strong); }
.tri { font: inherit; color: inherit; text-transform: inherit; letter-spacing: inherit; background: none; border: 0; padding: 0; cursor: pointer; display: inline-flex; gap: 0.3rem; }
.tri.on, .tri:hover { color: var(--ink); }
.fleche { display: inline-block; transition: rotate 0.25s var(--ease-out); }
.tri.on .fleche.asc { rotate: 180deg; }
.ligne { transition: background-color 0.15s; }
.ligne:hover, .ligne.ouvert { background: var(--surface-2); }
.deplier { font: inherit; color: inherit; background: none; border: 0; padding: 0; cursor: pointer; display: inline-flex; gap: 0.45rem; align-items: center; }
.chevron { display: inline-block; color: var(--muted); font-size: 1.1rem; transition: rotate 0.25s var(--ease-out); }
.ouvert .chevron { rotate: 90deg; color: var(--ink); }
.desc { max-width: 360px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--ink-2); }
.date { white-space: nowrap; color: var(--ink-2); }
.vide { text-align: center; color: var(--muted); padding: 2rem; }
.detail td { background: var(--bg-2); }
.meta { display: flex; flex-wrap: wrap; gap: 0.4rem 1.2rem; font-size: 0.85rem; color: var(--ink-2); margin-bottom: 0.6rem; }
pre { margin: 0; padding: 0.8rem; border-radius: var(--radius-sm); background: var(--surface); border: 1px solid var(--line); overflow: auto; max-height: 280px; color: var(--ink-2); }

.ligne-enter-active { transition: opacity 0.35s var(--ease-out), translate 0.35s var(--ease-out); transition-delay: calc(var(--i, 0) * 18ms); }
.ligne-enter-from { opacity: 0; }
.ligne-leave-active { display: none; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 1rem; }
</style>
